package com.pointbank.banking.transfer;

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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TransferIdempotencyIntegrationTest {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String REQUEST_NO = "TRF-transfer-001";

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

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    PasswordEncoder accountPasswordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM account_transactions");
        jdbcTemplate.update("DELETE FROM transfers");
        jdbcTemplate.update("DELETE FROM account_deposit_requests");
        jdbcTemplate.update("DELETE FROM accounts");
    }

    @Test
    void 같은_IdempotencyKey로_송금을_재요청해도_한번만_반영된다() throws Exception {
        createAccount(1L, "100000000001", 100000L, "1234");
        createAccount(2L, "100000000002", 0L, "5678");

        performTransfer()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fromBalanceAfter").value(70000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        performTransfer()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fromBalanceAfter").value(70000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        assertThat(accountBalance(1L)).isEqualTo(70000L);
        assertThat(accountBalance(2L)).isEqualTo(30000L);
        assertThat(queryLong("SELECT COUNT(*) FROM transfers")).isEqualTo(1L);

        Map<String, Object> transfer = jdbcTemplate.queryForMap("SELECT * FROM transfers");
        assertThat(transfer.get("status")).isEqualTo("COMPLETED");
        assertThat(transfer.get("request_no")).isEqualTo(REQUEST_NO);

        assertThat(queryLong("SELECT COUNT(*) FROM account_transactions")).isEqualTo(2L);
        assertThat(queryLong("""
                SELECT COUNT(*) FROM account_transactions
                WHERE transaction_type = 'TRANSFER_OUT'
                """)).isEqualTo(1L);
        assertThat(queryLong("""
                SELECT COUNT(*) FROM account_transactions
                WHERE transaction_type = 'TRANSFER_IN'
                """)).isEqualTo(1L);
        assertThat(queryLong("""
                SELECT COUNT(*) FROM account_transactions
                WHERE request_no = ?
                """, REQUEST_NO)).isEqualTo(2L);
        assertThat(queryLong("""
                SELECT balance_after FROM account_transactions
                WHERE transaction_type = 'TRANSFER_OUT'
                """)).isEqualTo(70000L);
        assertThat(queryLong("""
                SELECT balance_after FROM account_transactions
                WHERE transaction_type = 'TRANSFER_IN'
                """)).isEqualTo(30000L);
    }

    private org.springframework.test.web.servlet.ResultActions performTransfer() throws Exception {
        return mockMvc.perform(post("/api/banking/transfers")
                .header(MEMBER_ID_HEADER, "1")
                .header(IDEMPOTENCY_KEY_HEADER, "transfer-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "toAccountNumber": "100000000002",
                          "amount": 30000,
                          "accountPassword": "1234"
                        }
                        """));
    }

    private void createAccount(Long memberId, String accountNumber, long balance, String password) {
        jdbcTemplate.update("""
                INSERT INTO accounts (
                    member_id, account_number, account_password_hash, balance, status
                ) VALUES (?, ?, ?, ?, 'ACTIVE')
                """, memberId, accountNumber, accountPasswordEncoder.encode(password), balance);
    }

    private long accountBalance(Long memberId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE member_id = ?",
                Long.class,
                memberId
        );
    }

    private long queryLong(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }
}
