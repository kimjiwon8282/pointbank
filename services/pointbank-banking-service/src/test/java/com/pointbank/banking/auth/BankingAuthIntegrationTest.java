package com.pointbank.banking.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.banking.account.dto.AccountDepositRequest;
import com.pointbank.banking.account.domain.Account;
import com.pointbank.banking.account.mapper.AccountMapper;
import com.pointbank.banking.account.service.AccountService;
import com.pointbank.banking.transaction.domain.AccountTransaction;
import com.pointbank.banking.transaction.domain.AccountTransactionType;
import com.pointbank.banking.transaction.mapper.AccountTransactionMapper;
import com.pointbank.banking.transfer.domain.Transfer;
import com.pointbank.banking.transfer.domain.TransferStatus;
import com.pointbank.banking.transfer.mapper.TransferMapper;
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
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
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
    @Autowired
    AccountMapper accountMapper;
    @Autowired
    AccountTransactionMapper accountTransactionMapper;
    @Autowired
    TransferMapper transferMapper;
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
                        .header(IDEMPOTENCY_KEY_HEADER, "deposit-success")
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
        assertThat(transaction.get("transfer_id")).isNull();
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

    @Test
    void transferMapperStoresGeneratedIdAndFindsTransferByIdAndTransferNo() {
        Transfer transfer = Transfer.requested("TR-20260705-000001", 10L, 20L, 1L, 2L, 5000L);

        assertThat(transferMapper.insert(transfer)).isEqualTo(1);
        assertThat(transfer.getId()).isNotNull();

        Transfer byId = transferMapper.findById(transfer.getId()).orElseThrow();
        Transfer byTransferNo = transferMapper.findByTransferNo(transfer.getTransferNo()).orElseThrow();
        assertThat(byId.getTransferNo()).isEqualTo("TR-20260705-000001");
        assertThat(byId.getStatus()).isEqualTo(TransferStatus.REQUESTED);
        assertThat(byId.getAmount()).isEqualTo(5000L);
        assertThat(byId.getCreatedAt()).isNotNull();
        assertThat(byTransferNo.getId()).isEqualTo(transfer.getId());
    }

    @Test
    void accountMapperFindsByAccountNumberAndLocksAccountsInAscendingIdOrder() {
        createAccount(2L);
        createAccount(1L);
        Account memberTwoAccount = accountMapper.findByMemberId(2L).orElseThrow();
        Account memberOneAccount = accountMapper.findByMemberId(1L).orElseThrow();

        assertThat(accountMapper.findByAccountNumber(memberTwoAccount.getAccountNumber()))
                .get().extracting(Account::getId).isEqualTo(memberTwoAccount.getId());
        assertThat(accountMapper.findById(memberOneAccount.getId()))
                .get().extracting(Account::getMemberId).isEqualTo(1L);

        List<Account> lockedAccounts = accountMapper.findAllByIdsForUpdate(
                List.of(memberOneAccount.getId(), memberTwoAccount.getId()));
        assertThat(lockedAccounts).extracting(Account::getId).isSorted();
    }

    @Test
    void accountTransactionMapperStoresTransferTypesAndTransferId() {
        createAccount(1L);
        Long accountId = accountMapper.findByMemberId(1L).orElseThrow().getId();
        Transfer transfer = Transfer.requested("TR-20260705-000002", accountId, 20L, 1L, 2L, 1000L);
        transferMapper.insert(transfer);

        AccountTransaction transferOut = new AccountTransaction(
                accountId, 1L, transfer.getId(), AccountTransactionType.TRANSFER_OUT,
                1000L, 0L, "transfer out");
        AccountTransaction transferIn = new AccountTransaction(
                accountId, 1L, transfer.getId(), AccountTransactionType.TRANSFER_IN,
                1000L, 1000L, "transfer in");

        assertThat(accountTransactionMapper.insert(transferOut)).isEqualTo(1);
        assertThat(accountTransactionMapper.insert(transferIn)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT transaction_type FROM account_transactions ORDER BY id", String.class))
                .containsExactly("TRANSFER_OUT", "TRANSFER_IN");
        assertThat(jdbcTemplate.queryForList(
                "SELECT transfer_id FROM account_transactions ORDER BY id", Long.class))
                .containsExactly(transfer.getId(), transfer.getId());
    }

    @Test
    void transferCompletesAndStoresBalancesTransferAndTwoTransactions() throws Exception {
        String fromAccountNumber = createTransferAccount(1L, 100000L, "1234");
        String toAccountNumber = createTransferAccount(2L, 0L, "5678");

        mockMvc.perform(post("/api/banking/transfers")
                        .header(MEMBER_ID_HEADER, "1")
                        .header(IDEMPOTENCY_KEY_HEADER, "transfer-success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest(toAccountNumber, 10000L, "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("송금이 완료되었습니다."))
                .andExpect(jsonPath("$.data.transferNo").value(org.hamcrest.Matchers.matchesPattern("^TRF[0-9A-F]{32}$")))
                .andExpect(jsonPath("$.data.fromAccountNumber").value(fromAccountNumber))
                .andExpect(jsonPath("$.data.toAccountNumber").value(toAccountNumber))
                .andExpect(jsonPath("$.data.amount").value(10000))
                .andExpect(jsonPath("$.data.fromBalanceAfter").value(90000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        assertThat(accountBalance(1L)).isEqualTo(90000L);
        assertThat(accountBalance(2L)).isEqualTo(10000L);
        Map<String, Object> transfer = jdbcTemplate.queryForMap("SELECT * FROM transfers");
        assertThat(transfer.get("status")).isEqualTo("COMPLETED");
        assertThat(((Number) transfer.get("amount")).longValue()).isEqualTo(10000L);
        assertThat(transfer.get("completed_at")).isNotNull();
        List<Map<String, Object>> transactions = jdbcTemplate.queryForList(
                "SELECT * FROM account_transactions ORDER BY id");
        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting(row -> row.get("transaction_type"))
                .containsExactly("TRANSFER_OUT", "TRANSFER_IN");
        assertThat(transactions).extracting(row -> ((Number) row.get("balance_after")).longValue())
                .containsExactly(90000L, 10000L);
        assertThat(transactions).allSatisfy(row ->
                assertThat(((Number) row.get("transfer_id")).longValue())
                        .isEqualTo(((Number) transfer.get("id")).longValue()));
    }

    @Test
    void transferFailsWithoutChangesWhenBalanceIsInsufficient() throws Exception {
        createTransferAccount(1L, 5000L, "1234");
        String toAccountNumber = createTransferAccount(2L, 0L, "5678");

        performTransfer(1L, toAccountNumber, 10000L, "1234")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
        assertTransferFailureState(5000L, 0L);
    }

    @Test
    void transferFailsWithoutChangesWhenPasswordDoesNotMatch() throws Exception {
        createTransferAccount(1L, 100000L, "1234");
        String toAccountNumber = createTransferAccount(2L, 0L, "5678");

        performTransfer(1L, toAccountNumber, 10000L, "9999")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ACCOUNT_PASSWORD"));
        assertTransferFailureState(100000L, 0L);
    }

    @Test
    void transferFailsWhenReceiverAccountDoesNotExist() throws Exception {
        createTransferAccount(1L, 100000L, "1234");

        performTransfer(1L, "999999999999", 10000L, "1234")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECEIVER_ACCOUNT_NOT_FOUND"));
        assertThat(accountBalance(1L)).isEqualTo(100000L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transfers", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account_transactions", Integer.class)).isZero();
    }

    @Test
    void transferFailsWhenSendingToSameAccount() throws Exception {
        String accountNumber = createTransferAccount(1L, 100000L, "1234");

        performTransfer(1L, accountNumber, 10000L, "1234")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_TRANSFER_TO_SELF"));
        assertThat(accountBalance(1L)).isEqualTo(100000L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transfers", Integer.class)).isZero();
    }

    @Test
    void transferRejectsZeroAndNegativeAmounts() throws Exception {
        createTransferAccount(1L, 100000L, "1234");
        String toAccountNumber = createTransferAccount(2L, 0L, "5678");

        performTransfer(1L, toAccountNumber, 0L, "1234").andExpect(status().isBadRequest());
        performTransfer(1L, toAccountNumber, -1L, "1234").andExpect(status().isBadRequest());
        assertTransferFailureState(100000L, 0L);
    }

    @Test
    void transferFailsWithoutChangesWhenAccountIsInactive() throws Exception {
        createTransferAccount(1L, 100000L, "1234");
        String toAccountNumber = createTransferAccount(2L, 0L, "5678");
        jdbcTemplate.update("UPDATE accounts SET status = 'SUSPENDED' WHERE member_id = 2");

        performTransfer(1L, toAccountNumber, 10000L, "1234")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));
        assertTransferFailureState(100000L, 0L);
    }

    @Test
    void transactionHistoryReturnsCurrentAccountsTransferWithCounterparty() throws Exception {
        createTransferAccount(1L, 100000L, "1234");
        String toAccountNumber = createTransferAccount(2L, 0L, "5678");
        performTransfer(1L, toAccountNumber, 10000L, "1234").andExpect(status().isOk());

        mockMvc.perform(get("/api/banking/transactions")
                        .header(MEMBER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("거래내역 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].transactionType").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.data.items[0].direction").value("OUT"))
                .andExpect(jsonPath("$.data.items[0].signedAmount").value(-10000))
                .andExpect(jsonPath("$.data.items[0].counterpartyAccountNumber").value(toAccountNumber))
                .andExpect(jsonPath("$.data.items[0].transferNo").isNotEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursorCreatedAt").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.nextCursorId").value(org.hamcrest.Matchers.nullValue()));
    }

    private org.springframework.test.web.servlet.ResultActions performTransfer(
            Long memberId, String toAccountNumber, Long amount, String password) throws Exception {
        return mockMvc.perform(post("/api/banking/transfers")
                .header(MEMBER_ID_HEADER, memberId.toString())
                .header(IDEMPOTENCY_KEY_HEADER, "transfer-" + System.nanoTime())
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferRequest(toAccountNumber, amount, password)));
    }

    private String transferRequest(String toAccountNumber, Long amount, String password) {
        return "{\"toAccountNumber\":\"" + toAccountNumber + "\",\"amount\":" + amount
                + ",\"accountPassword\":\"" + password + "\"}";
    }

    private String createTransferAccount(Long memberId, long balance, String password) {
        String accountNumber = String.format("200000000%03d", memberId);
        jdbcTemplate.update("""
                INSERT INTO accounts (
                    member_id, account_number, account_password_hash, balance, status
                ) VALUES (?, ?, ?, ?, 'ACTIVE')
                """, memberId, accountNumber, accountPasswordEncoder.encode(password), balance);
        return accountNumber;
    }

    private void assertTransferFailureState(long fromBalance, long toBalance) {
        assertThat(accountBalance(1L)).isEqualTo(fromBalance);
        assertThat(accountBalance(2L)).isEqualTo(toBalance);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transfers", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account_transactions", Integer.class)).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions deposit(Long memberId, Long amount) throws Exception {
        return mockMvc.perform(post("/api/banking/accounts/deposit")
                .header(MEMBER_ID_HEADER, memberId.toString())
                .header(IDEMPOTENCY_KEY_HEADER, "deposit-" + System.nanoTime())
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
