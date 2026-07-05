package com.pointbank.banking.transfer;

import com.pointbank.banking.global.exception.CustomException;
import com.pointbank.banking.global.exception.ErrorCode;
import com.pointbank.banking.transfer.dto.TransferCreateRequest;
import com.pointbank.banking.transfer.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class TransferConcurrencyIntegrationTest {
    private static final String PASSWORD_A = "1234";
    private static final String PASSWORD_B = "5678";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("banking_db")
            .withUsername("pointbank")
            .withPassword("pointbank")
            .withInitScript("schema-banking-test.sql");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired TransferService transferService;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder accountPasswordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM account_transactions");
        jdbcTemplate.update("DELETE FROM transfers");
        jdbcTemplate.update("DELETE FROM accounts");
    }

    @Test
    void concurrentTransfersFromSameAccountPreserveBalancesAndHistory() throws Exception {
        createAccount(1L, "100000000001", 100_000L, PASSWORD_A);
        createAccount(2L, "100000000002", 0L, PASSWORD_B);

        List<TransferOutcome> outcomes = runConcurrently(
                tasks(20, 1L, "100000000002", 10_000L, PASSWORD_A));

        assertOutcomes(outcomes, 10, ErrorCode.INSUFFICIENT_BALANCE, 10);
        assertThat(balance(1L)).isZero();
        assertThat(balance(2L)).isEqualTo(100_000L);
        assertThat(count("transfers")).isEqualTo(10);
        assertThat(countWhere("transfers", "status", "COMPLETED")).isEqualTo(10);
        assertThat(count("account_transactions")).isEqualTo(20);
        assertThat(countWhere("account_transactions", "transaction_type", "TRANSFER_OUT")).isEqualTo(10);
        assertThat(countWhere("account_transactions", "transaction_type", "TRANSFER_IN")).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT MIN(balance_after) FROM account_transactions
                WHERE transaction_type = 'TRANSFER_OUT'
                """, Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE balance < 0", Integer.class)).isZero();
    }

    @Test
    void bidirectionalConcurrentTransfersCompleteWithoutDeadlock() throws Exception {
        createAccount(1L, "100000000001", 100_000L, PASSWORD_A);
        createAccount(2L, "100000000002", 100_000L, PASSWORD_B);
        List<Callable<TransferOutcome>> requests = new ArrayList<>();
        requests.addAll(tasks(20, 1L, "100000000002", 1_000L, PASSWORD_A));
        requests.addAll(tasks(20, 2L, "100000000001", 1_000L, PASSWORD_B));

        List<TransferOutcome> outcomes = runConcurrently(requests);

        assertOutcomes(outcomes, 40, null, 0);
        assertThat(balance(1L)).isEqualTo(100_000L);
        assertThat(balance(2L)).isEqualTo(100_000L);
        assertThat(count("transfers")).isEqualTo(40);
        assertThat(count("account_transactions")).isEqualTo(80);
        assertThat(countWhere("account_transactions", "transaction_type", "TRANSFER_OUT")).isEqualTo(40);
        assertThat(countWhere("account_transactions", "transaction_type", "TRANSFER_IN")).isEqualTo(40);
    }

    @Test
    void failedConcurrentTransfersLeaveNoPartialTransferOrHistory() throws Exception {
        createAccount(1L, "100000000001", 30_000L, PASSWORD_A);
        createAccount(2L, "100000000002", 0L, PASSWORD_B);

        List<TransferOutcome> outcomes = runConcurrently(
                tasks(10, 1L, "100000000002", 10_000L, PASSWORD_A));

        assertOutcomes(outcomes, 3, ErrorCode.INSUFFICIENT_BALANCE, 7);
        assertThat(balance(1L)).isZero();
        assertThat(balance(2L)).isEqualTo(30_000L);
        assertThat(count("transfers")).isEqualTo(3);
        assertThat(countWhere("transfers", "status", "COMPLETED")).isEqualTo(3);
        assertThat(countWhere("transfers", "status", "FAILED")).isZero();
        assertThat(count("account_transactions")).isEqualTo(6);
    }

    @Test
    void concurrentWrongPasswordsLeaveDatabaseUnchanged() throws Exception {
        createAccount(1L, "100000000001", 100_000L, PASSWORD_A);
        createAccount(2L, "100000000002", 0L, PASSWORD_B);

        List<TransferOutcome> outcomes = runConcurrently(
                tasks(10, 1L, "100000000002", 10_000L, "9999"));

        assertOutcomes(outcomes, 0, ErrorCode.INVALID_ACCOUNT_PASSWORD, 10);
        assertThat(balance(1L)).isEqualTo(100_000L);
        assertThat(balance(2L)).isZero();
        assertThat(count("transfers")).isZero();
        assertThat(count("account_transactions")).isZero();
    }

    private List<Callable<TransferOutcome>> tasks(
            int count, Long memberId, String target, long amount, String password) {
        TransferCreateRequest request = new TransferCreateRequest(target, amount, password);
        List<Callable<TransferOutcome>> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            result.add(() -> execute(memberId, request));
        }
        return result;
    }

    private TransferOutcome execute(Long memberId, TransferCreateRequest request) {
        try {
            transferService.transfer(memberId, request);
            return TransferOutcome.succeeded();
        } catch (CustomException exception) {
            return TransferOutcome.failed(exception.getErrorCode());
        }
    }

    private List<TransferOutcome> runConcurrently(List<Callable<TransferOutcome>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<TransferOutcome>> futures = tasks.stream().map(task -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent start timed out");
                }
                return task.call();
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<TransferOutcome> outcomes = new ArrayList<>();
            for (Future<TransferOutcome> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void assertOutcomes(
            List<TransferOutcome> outcomes, int successes, ErrorCode error, int failures) {
        assertThat(outcomes).filteredOn(TransferOutcome::success).hasSize(successes);
        assertThat(outcomes).filteredOn(outcome -> !outcome.success()).hasSize(failures);
        if (error != null) {
            assertThat(outcomes).filteredOn(outcome -> !outcome.success())
                    .extracting(TransferOutcome::errorCode).containsOnly(error);
        }
    }

    private void createAccount(Long memberId, String number, long balance, String password) {
        jdbcTemplate.update("""
                INSERT INTO accounts
                    (member_id, account_number, account_password_hash, balance, status)
                VALUES (?, ?, ?, ?, 'ACTIVE')
                """, memberId, number, accountPasswordEncoder.encode(password), balance);
    }

    private long balance(Long memberId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE member_id = ?", Long.class, memberId);
    }

    private int count(String table) {
        if (table.equals("transfers")) {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transfers", Integer.class);
        }
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account_transactions", Integer.class);
    }

    private int countWhere(String table, String column, String value) {
        String sql;
        if (table.equals("transfers")) {
            sql = "SELECT COUNT(*) FROM transfers WHERE status = ?";
        } else {
            sql = "SELECT COUNT(*) FROM account_transactions WHERE transaction_type = ?";
        }
        return jdbcTemplate.queryForObject(sql, Integer.class, value);
    }

    private record TransferOutcome(boolean success, ErrorCode errorCode) {
        static TransferOutcome succeeded() { return new TransferOutcome(true, null); }
        static TransferOutcome failed(ErrorCode code) { return new TransferOutcome(false, code); }
    }
}
