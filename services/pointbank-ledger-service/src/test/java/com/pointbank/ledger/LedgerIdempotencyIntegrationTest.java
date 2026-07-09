package com.pointbank.ledger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class LedgerIdempotencyIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("ledger_db")
            .withUsername("pointbank")
            .withPassword("pointbank")
            .withInitScript("schema-ledger-test.sql");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM ledger_entries");
        jdbcTemplate.update("DELETE FROM ledger_transfer_requests");
        jdbcTemplate.update("DELETE FROM ledger_accounts");
    }

    @Test
    void 같은_IdempotencyKey로_계좌_충전을_재요청해도_한번만_반영된다() throws Exception {
        createBankingAccount(1L, "100000000001", 0L, "1234");

        performDeposit("deposit-001", 1L, 50000L).andExpect(status().isOk());
        performDeposit("deposit-001", 1L, 50000L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceAfter").value(50000));

        assertThat(balance(1L, "BANKING")).isEqualTo(50000L);
        assertThat(count("ledger_transfer_requests")).isEqualTo(1L);
        assertThat(count("ledger_entries")).isEqualTo(1L);
        assertThat(countWhere("ledger_transfer_requests", "request_no", "ACCDEP-deposit-001")).isEqualTo(1L);
        assertThat(countWhere("ledger_entries", "request_no", "ACCDEP-deposit-001")).isEqualTo(1L);
    }

    @Test
    void 같은_IdempotencyKey로_송금을_재요청해도_한번만_반영된다() throws Exception {
        createBankingAccount(1L, "100000000001", 100000L, "1234");
        createBankingAccount(2L, "100000000002", 0L, "5678");

        performTransfer("transfer-001").andExpect(status().isOk());
        performTransfer("transfer-001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fromBalanceAfter").value(70000));

        assertThat(balance(1L, "BANKING")).isEqualTo(70000L);
        assertThat(balance(2L, "BANKING")).isEqualTo(30000L);
        assertThat(count("ledger_transfer_requests")).isEqualTo(1L);
        assertThat(count("ledger_entries")).isEqualTo(2L);
        assertThat(countEntry("TRF-transfer-001", "TRANSFER_OUT")).isEqualTo(1L);
        assertThat(countEntry("TRF-transfer-001", "TRANSFER_IN")).isEqualTo(1L);
        assertThat(countWhere("ledger_entries", "request_no", "TRF-transfer-001")).isEqualTo(2L);
    }

    @Test
    void 같은_IdempotencyKey로_예수금_충전을_재요청해도_한번만_반영된다() throws Exception {
        createBankingAccount(1L, "100000000001", 100000L, "1234");
        createSecuritiesCashAccount(1L);

        performSecuritiesDeposit("secdep-001").andExpect(status().isOk());
        performSecuritiesDeposit("secdep-001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bankingBalanceAfter").value(70000))
                .andExpect(jsonPath("$.data.cashBalance").value(30000));

        assertThat(balance(1L, "BANKING")).isEqualTo(70000L);
        assertThat(balance(1L, "SECURITIES_CASH")).isEqualTo(30000L);
        assertThat(count("ledger_transfer_requests")).isEqualTo(1L);
        assertThat(count("ledger_entries")).isEqualTo(2L);
        assertThat(countEntry("SECDEP-secdep-001", "SECURITIES_DEPOSIT_OUT")).isEqualTo(1L);
        assertThat(countEntry("SECDEP-secdep-001", "SECURITIES_DEPOSIT_IN")).isEqualTo(1L);
    }

    private org.springframework.test.web.servlet.ResultActions performDeposit(String key, Long memberId, long amount) throws Exception {
        return mockMvc.perform(post("/internal/ledger/banking/accounts/deposit")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"amount\":" + amount + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions performTransfer(String key) throws Exception {
        return mockMvc.perform(post("/internal/ledger/banking/transfers")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"memberId":1,"toAccountNumber":"100000000002","amount":30000,"accountPassword":"1234"}
                        """));
    }

    private org.springframework.test.web.servlet.ResultActions performSecuritiesDeposit(String key) throws Exception {
        return mockMvc.perform(post("/internal/ledger/securities/cash/deposit")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"memberId":1,"amount":30000,"accountPassword":"1234"}
                        """));
    }

    private void createBankingAccount(Long memberId, String accountNumber, long balance, String password) {
        jdbcTemplate.update("""
                INSERT INTO ledger_accounts (
                    member_id, account_type, account_number, account_password_hash,
                    balance, reserved_balance, status
                ) VALUES (?, 'BANKING', ?, ?, ?, 0, 'ACTIVE')
                """, memberId, accountNumber, passwordEncoder.encode(password), balance);
    }

    private void createSecuritiesCashAccount(Long memberId) {
        jdbcTemplate.update("""
                INSERT INTO ledger_accounts (
                    member_id, account_type, balance, reserved_balance, status
                ) VALUES (?, 'SECURITIES_CASH', 0, 0, 'ACTIVE')
                """, memberId);
    }

    private long balance(Long memberId, String type) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM ledger_accounts WHERE member_id = ? AND account_type = ?",
                Long.class, memberId, type);
    }

    private long count(String table) {
        if (table.equals("ledger_transfer_requests")) {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_transfer_requests", Long.class);
        }
        if (table.equals("ledger_entries")) {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_entries", Long.class);
        }
        throw new IllegalArgumentException("Unsupported table: " + table);
    }

    private long countWhere(String table, String column, String value) {
        if (table.equals("ledger_transfer_requests") && column.equals("request_no")) {
            return jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ledger_transfer_requests WHERE request_no = ?",
                    Long.class, value);
        }
        if (table.equals("ledger_entries") && column.equals("request_no")) {
            return jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ledger_entries WHERE request_no = ?",
                    Long.class, value);
        }
        throw new IllegalArgumentException("Unsupported countWhere");
    }

    private long countEntry(String requestNo, String entryType) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_entries WHERE request_no = ? AND entry_type = ?",
                Long.class, requestNo, entryType);
    }
}
