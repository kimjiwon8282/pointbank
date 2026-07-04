package com.pointbank.banking.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.banking.account.dto.AccountDepositRequest;
import com.pointbank.banking.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class BankingAuthIntegrationTest {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String ROLE_HEADER = "X-Role";

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
    ObjectMapper objectMapper;
    @Autowired
    AccountService accountService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM account_transactions");
        jdbcTemplate.update("DELETE FROM accounts");
    }

    @Test
    void 유효한_내부_인증_헤더가_있으면_현재_회원_정보를_반환한다() throws Exception {
        mockMvc.perform(get("/api/banking/me")
                        .header(MEMBER_ID_HEADER, "1")
                        .header(ROLE_HEADER, "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("뱅킹 인증 정보 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void X_Member_Id가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/banking/me").header(ROLE_HEADER, "USER"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void X_Member_Id가_숫자가_아니면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/banking/me")
                        .header(MEMBER_ID_HEADER, "not-a-number")
                        .header(ROLE_HEADER, "USER"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void X_Role이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/banking/me").header(MEMBER_ID_HEADER, "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void X_Role이_빈_문자열이면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/banking/me")
                        .header(MEMBER_ID_HEADER, "1")
                        .header(ROLE_HEADER, ""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 계좌_개설에_성공하면_초기_계좌_정보를_반환하고_비밀번호는_BCrypt로_저장한다() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/banking/accounts")
                        .header(MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountPassword\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("계좌 개설에 성공했습니다."))
                .andExpect(jsonPath("$.data.accountId").isNumber())
                .andExpect(jsonPath("$.data.accountNumber").isString())
                .andExpect(jsonPath("$.data.balance").value(0))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("data").path("accountNumber").asText()).matches("^[0-9]{12}$");
        Map<String, Object> account = jdbcTemplate.queryForMap(
                "SELECT * FROM accounts WHERE member_id = 1"
        );
        assertThat(account.get("account_password_hash").toString())
                .startsWith("$2")
                .isNotEqualTo("1234");
        assertThat(((Number) account.get("balance")).longValue()).isZero();
        assertThat(account.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    void 계좌_비밀번호가_누락되면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/banking/accounts")
                        .header(MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 계좌_비밀번호가_숫자_4자리가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/banking/accounts")
                        .header(MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountPassword\":\"12ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 이미_계좌가_있는_회원이_다시_개설하면_409를_반환한다() throws Exception {
        createAccount(1L);

        mockMvc.perform(post("/api/banking/accounts")
                        .header(MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountPassword\":\"5678\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ALREADY_EXISTS"));
    }

    @Test
    void 내_계좌_조회에_성공한다() throws Exception {
        createAccount(1L);

        mockMvc.perform(get("/api/banking/accounts/me").header(MEMBER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("내 계좌 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.accountId").isNumber())
                .andExpect(jsonPath("$.data.balance").value(0))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void 내_계좌가_없으면_404를_반환한다() throws Exception {
        mockMvc.perform(get("/api/banking/accounts/me").header(MEMBER_ID_HEADER, "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void 계좌_API에_X_Member_Id가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/banking/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 계좌_API의_X_Member_Id가_숫자가_아니면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/banking/accounts/me").header(MEMBER_ID_HEADER, "invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 개발용_충전에_성공하면_잔액과_DEPOSIT_거래내역을_저장한다() throws Exception {
        createAccount(1L);

        mockMvc.perform(post("/api/banking/accounts/deposit")
                        .header(MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("개발용 포인트 충전이 완료되었습니다."))
                .andExpect(jsonPath("$.data.amount").value(100000))
                .andExpect(jsonPath("$.data.balanceAfter").value(100000))
                .andExpect(jsonPath("$.data.transactionType").value("DEPOSIT"));

        assertThat(accountBalance(1L)).isEqualTo(100000L);
        Map<String, Object> transaction = jdbcTemplate.queryForMap(
                "SELECT * FROM account_transactions WHERE member_id = 1"
        );
        assertThat(transaction.get("transaction_type")).isEqualTo("DEPOSIT");
        assertThat(((Number) transaction.get("amount")).longValue()).isEqualTo(100000L);
        assertThat(((Number) transaction.get("balance_after")).longValue()).isEqualTo(100000L);
        assertThat(transaction.get("description")).isEqualTo("개발용 포인트 충전");
    }

    @Test
    void 추가_충전은_잔액을_누적하고_거래내역을_각각_저장한다() throws Exception {
        createAccount(1L);
        deposit(1L, 100000L);

        deposit(1L, 50000L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceAfter").value(150000));

        assertThat(accountBalance(1L)).isEqualTo(150000L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_transactions WHERE member_id = 1",
                Integer.class
        )).isEqualTo(2);
    }

    @Test
    void 충전할_계좌가_없으면_404를_반환한다() throws Exception {
        deposit(1L, 1000L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void 충전_금액이_누락되면_400을_반환한다() throws Exception {
        createAccount(1L);
        mockMvc.perform(post("/api/banking/accounts/deposit")
                        .header(MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 충전_금액이_0이면_400을_반환한다() throws Exception {
        createAccount(1L);
        deposit(1L, 0L)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 충전_금액이_음수이면_400을_반환한다() throws Exception {
        createAccount(1L);
        deposit(1L, -1L)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 충전_금액이_1회_한도를_초과하면_400을_반환한다() throws Exception {
        createAccount(1L);
        deposit(1L, 1000001L)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 충전_API에_X_Member_Id가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/banking/accounts/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 충전_API의_X_Member_Id가_숫자가_아니면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/banking/accounts/deposit")
                        .header(MEMBER_ID_HEADER, "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void ACTIVE가_아닌_계좌에는_충전할_수_없다() throws Exception {
        createAccount(1L);
        jdbcTemplate.update("UPDATE accounts SET status = 'SUSPENDED' WHERE member_id = 1");

        deposit(1L, 1000L)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));
        assertThat(accountBalance(1L)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account_transactions", Integer.class))
                .isZero();
    }

    @Test
    void 같은_계좌에_20건을_동시에_충전해도_잔액과_거래내역이_일치한다() throws Exception {
        createAccount(1L);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        try {
            List<Callable<Void>> tasks = java.util.stream.IntStream.range(0, 20)
                    .mapToObj(index -> (Callable<Void>) () -> {
                        accountService.deposit(1L, new AccountDepositRequest(1000L));
                        return null;
                    })
                    .toList();
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(accountBalance(1L)).isEqualTo(20000L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account_transactions", Integer.class))
                .isEqualTo(20);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT MAX(balance_after) FROM account_transactions",
                Long.class
        )).isEqualTo(20000L);
    }

    private org.springframework.test.web.servlet.ResultActions deposit(Long memberId, Long amount) throws Exception {
        return mockMvc.perform(post("/api/banking/accounts/deposit")
                .header(MEMBER_ID_HEADER, memberId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":" + amount + "}"));
    }

    private long accountBalance(Long memberId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE member_id = ?",
                Long.class,
                memberId
        );
    }

    private void createAccount(Long memberId) {
        jdbcTemplate.update("""
                INSERT INTO accounts (
                    member_id, account_number, account_password_hash, balance, status
                ) VALUES (?, ?, ?, 0, 'ACTIVE')
                """, memberId, "10000000000" + memberId, "$2a$10$test-password-hash");
    }
}
