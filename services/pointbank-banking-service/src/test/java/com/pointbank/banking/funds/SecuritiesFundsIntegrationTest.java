package com.pointbank.banking.funds;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class SecuritiesFundsIntegrationTest {

    private static final String PATH = "/api/banking/funds/securities/deposit";
    private static final String MEMBER_HEADER = "X-Member-Id";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

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

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder accountPasswordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM securities_cash_transactions");
        jdbcTemplate.update("DELETE FROM fund_transfer_requests");
        jdbcTemplate.update("DELETE FROM account_transactions");
        jdbcTemplate.update("DELETE FROM transfers");
        jdbcTemplate.update("DELETE FROM securities_cash_accounts");
        jdbcTemplate.update("DELETE FROM accounts");
    }

    @Test
    void 같은_IdempotencyKey로_예수금_충전을_재요청해도_한번만_반영된다() throws Exception {
        createBankingAccount();
        createSecuritiesCashAccount();

        String body = """
                {
                  "amount": 30000,
                  "accountPassword": "1234"
                }
                """;

        mockMvc.perform(post(PATH)
                        .header(MEMBER_HEADER, "1")
                        .header(IDEMPOTENCY_HEADER, "dep-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestNo").value("SECDEP-dep-001"))
                .andExpect(jsonPath("$.data.amount").value(30000))
                .andExpect(jsonPath("$.data.bankingBalanceAfter").value(70000))
                .andExpect(jsonPath("$.data.cashBalance").value(30000))
                .andExpect(jsonPath("$.data.reservedCash").value(0))
                .andExpect(jsonPath("$.data.availableCash").value(30000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(post(PATH)
                        .header(MEMBER_HEADER, "1")
                        .header(IDEMPOTENCY_HEADER, "dep-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestNo").value("SECDEP-dep-001"))
                .andExpect(jsonPath("$.data.amount").value(30000))
                .andExpect(jsonPath("$.data.bankingBalanceAfter").value(70000))
                .andExpect(jsonPath("$.data.cashBalance").value(30000))
                .andExpect(jsonPath("$.data.availableCash").value(30000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        assertThat(queryLong("SELECT balance FROM accounts WHERE member_id = 1")).isEqualTo(70000L);
        assertThat(queryLong("SELECT cash_balance FROM securities_cash_accounts WHERE member_id = 1")).isEqualTo(30000L);
        assertThat(queryLong("SELECT reserved_cash FROM securities_cash_accounts WHERE member_id = 1")).isEqualTo(0L);

        assertThat(queryLong("SELECT COUNT(*) FROM fund_transfer_requests")).isEqualTo(1L);
        assertThat(queryLong("SELECT COUNT(*) FROM account_transactions")).isEqualTo(1L);
        assertThat(queryLong("SELECT COUNT(*) FROM securities_cash_transactions")).isEqualTo(1L);

        assertThat(queryString("SELECT request_no FROM fund_transfer_requests")).isEqualTo("SECDEP-dep-001");
        assertThat(queryString("SELECT request_no FROM account_transactions")).isEqualTo("SECDEP-dep-001");
        assertThat(queryString("SELECT request_no FROM securities_cash_transactions")).isEqualTo("SECDEP-dep-001");
        assertThat(queryString("SELECT transaction_type FROM account_transactions")).isEqualTo("SECURITIES_DEPOSIT_OUT");
        assertThat(queryString("SELECT transaction_type FROM securities_cash_transactions")).isEqualTo("DEPOSIT_FROM_BANKING");
    }

    private void createBankingAccount() {
        jdbcTemplate.update("""
                INSERT INTO accounts
                    (member_id, account_number, account_password_hash, balance, status)
                VALUES (1, '200000000001', ?, 100000, 'ACTIVE')
                """, accountPasswordEncoder.encode("1234"));
    }

    private void createSecuritiesCashAccount() {
        jdbcTemplate.update("""
                INSERT INTO securities_cash_accounts
                    (member_id, cash_balance, reserved_cash, status)
                VALUES (1, 0, 0, 'ACTIVE')
                """);
    }

    private Long queryLong(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    private String queryString(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }
}
