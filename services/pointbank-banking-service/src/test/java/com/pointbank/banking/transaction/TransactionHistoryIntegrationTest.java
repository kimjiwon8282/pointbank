package com.pointbank.banking.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TransactionHistoryIntegrationTest {
    private static final String PATH = "/api/banking/transactions";
    private static final String MEMBER_HEADER = "X-Member-Id";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("banking_db").withUsername("pointbank").withPassword("pointbank")
            .withInitScript("schema-banking-test.sql");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM account_transactions");
        jdbcTemplate.update("DELETE FROM transfers");
        jdbcTemplate.update("DELETE FROM accounts");
    }

    @Test
    void defaultQueryReturnsRecentDeposit() throws Exception {
        long accountId = createAccount(1L);
        createTransaction(accountId, 1L, null, "DEPOSIT", 1000, 1000,
                null, LocalDateTime.now().minusDays(1));

        mockMvc.perform(get(PATH).header(MEMBER_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("거래내역 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.items[0].direction").value("IN"))
                .andExpect(jsonPath("$.data.items[0].signedAmount").value(1000))
                .andExpect(jsonPath("$.data.items[0].counterpartyAccountNumber").doesNotExist())
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursorCreatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.nextCursorId").doesNotExist());
    }

    @Test
    void ordersByCreatedAtDescending() throws Exception {
        long accountId = createAccount(1L);
        long oldest = createDeposit(accountId, 1L, LocalDateTime.of(2026, 7, 1, 10, 0));
        long newest = createDeposit(accountId, 1L, LocalDateTime.of(2026, 7, 3, 10, 0));
        long middle = createDeposit(accountId, 1L, LocalDateTime.of(2026, 7, 2, 10, 0));

        assertThat(ids(getHistory(1L, "from", "2026-07-01", "to", "2026-07-31")))
                .containsExactly(newest, middle, oldest);
    }

    @Test
    void ordersByIdDescendingWhenCreatedAtIsEqual() throws Exception {
        long accountId = createAccount(1L);
        LocalDateTime sameTime = LocalDateTime.of(2026, 7, 5, 12, 0);
        long first = createDeposit(accountId, 1L, sameTime);
        long second = createDeposit(accountId, 1L, sameTime);
        long third = createDeposit(accountId, 1L, sameTime);

        assertThat(ids(getHistory(1L, "from", "2026-07-01", "to", "2026-07-31")))
                .containsExactly(third, second, first);
    }

    @Test
    void appliesInclusiveDateBoundaries() throws Exception {
        long accountId = createAccount(1L);
        createDeposit(accountId, 1L, LocalDateTime.of(2026, 6, 30, 23, 59, 59, 999_999_000));
        long start = createDeposit(accountId, 1L, LocalDateTime.of(2026, 7, 1, 0, 0));
        long end = createDeposit(accountId, 1L, LocalDateTime.of(2026, 7, 31, 23, 59, 59));
        createDeposit(accountId, 1L, LocalDateTime.of(2026, 8, 1, 0, 0));

        assertThat(ids(getHistory(1L, "from", "2026-07-01", "to", "2026-07-31")))
                .containsExactly(end, start);
    }

    @Test
    void fromOnlyUsesTodayAsEndDate() throws Exception {
        long accountId = createAccount(1L);
        LocalDate from = LocalDate.now().minusDays(7);
        createDeposit(accountId, 1L, from.minusDays(1).atStartOfDay());
        long included = createDeposit(accountId, 1L, from.atStartOfDay());

        assertThat(ids(getHistory(1L, "from", from.toString()))).containsExactly(included);
    }

    @Test
    void toOnlyUsesOneMonthEarlierAsStartDate() throws Exception {
        long accountId = createAccount(1L);
        createDeposit(accountId, 1L, LocalDateTime.of(2026, 6, 29, 23, 59));
        long monthStart = createDeposit(accountId, 1L, LocalDateTime.of(2026, 6, 30, 0, 0));
        long first = createDeposit(accountId, 1L, LocalDateTime.of(2026, 7, 1, 0, 0));
        long last = createDeposit(accountId, 1L, LocalDateTime.of(2026, 7, 31, 23, 59));
        createDeposit(accountId, 1L, LocalDateTime.of(2026, 8, 1, 0, 0));

        assertThat(ids(getHistory(1L, "to", "2026-07-31")))
                .containsExactly(last, first, monthStart);
    }

    @Test
    void rejectsReversedDateRange() throws Exception {
        expectBadRequest("from", "2026-08-01", "to", "2026-07-01");
    }

    @Test
    void filtersAllInAndOutTypes() throws Exception {
        long accountId = createAccount(1L);
        long otherId = createAccount(2L);
        long transferId = createTransfer(accountId, otherId);
        LocalDateTime now = LocalDateTime.now();
        createTransaction(accountId, 1L, null, "DEPOSIT", 100, 100, null, now.minusSeconds(3));
        createTransaction(accountId, 1L, transferId, "TRANSFER_IN", 100, 200, null, now.minusSeconds(2));
        createTransaction(accountId, 1L, transferId, "TRANSFER_OUT", 100, 100, null, now.minusSeconds(1));

        assertThat(types(getHistory(1L, "type", "ALL")))
                .containsExactly("TRANSFER_OUT", "TRANSFER_IN", "DEPOSIT");
        JsonNode in = getHistory(1L, "type", "IN");
        assertThat(types(in)).containsExactly("TRANSFER_IN", "DEPOSIT");
        assertThat(values(in, "direction")).containsOnly("IN");
        assertThat(longValues(in, "signedAmount")).allMatch(value -> value > 0);
        JsonNode out = getHistory(1L, "type", "OUT");
        assertThat(types(out)).containsExactly("TRANSFER_OUT");
        assertThat(out.at("/data/items/0/direction").asText()).isEqualTo("OUT");
        assertThat(out.at("/data/items/0/signedAmount").asLong()).isNegative();
    }

    @Test
    void rejectsInvalidType() throws Exception {
        expectBadRequest("type", "INVALID");
    }

    @Test
    void firstKeysetPageReturnsCursorFromLastItem() throws Exception {
        long accountId = createAccount(1L);
        LocalDateTime now = LocalDateTime.now();
        createDeposit(accountId, 1L, now.minusMinutes(3));
        createDeposit(accountId, 1L, now.minusMinutes(2));
        createDeposit(accountId, 1L, now.minusMinutes(1));

        JsonNode page = getHistory(1L, "size", "2");
        assertThat(page.at("/data/items")).hasSize(2);
        assertThat(page.at("/data/hasNext").asBoolean()).isTrue();
        assertThat(page.at("/data/nextCursorCreatedAt").asText())
                .isEqualTo(page.at("/data/items/1/createdAt").asText());
        assertThat(page.at("/data/nextCursorId").asLong())
                .isEqualTo(page.at("/data/items/1/transactionId").asLong());
    }

    @Test
    void keysetPagesContainEveryTransactionWithoutDuplicates() throws Exception {
        long accountId = createAccount(1L);
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < 5; index++) {
            createDeposit(accountId, 1L, now.minusMinutes(index));
        }

        List<Long> allIds = readAllPages(1L, 2);
        assertThat(allIds).hasSize(5).doesNotHaveDuplicates();
    }

    @Test
    void keysetUsesIdWhenAllCreatedAtValuesAreEqual() throws Exception {
        long accountId = createAccount(1L);
        LocalDateTime sameTime = LocalDateTime.now().minusDays(1).withNano(123_456_000);
        List<Long> inserted = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            inserted.add(createDeposit(accountId, 1L, sameTime));
        }

        List<Long> expected = inserted.reversed();
        assertThat(readAllPages(1L, 2)).containsExactlyElementsOf(expected);
    }

    @Test
    void rejectsIncompleteCursorPairs() throws Exception {
        expectBadRequest("cursorCreatedAt", "2026-07-05T12:00:00");
        expectBadRequest("cursorId", "10");
    }

    @Test
    void validatesSizeBoundaries() throws Exception {
        createAccount(1L);
        mockMvc.perform(get(PATH).header(MEMBER_HEADER, "1").param("size", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get(PATH).header(MEMBER_HEADER, "1").param("size", "50"))
                .andExpect(status().isOk());
        expectBadRequest("size", "0");
        expectBadRequest("size", "51");
        expectBadRequest("size", "-1");
    }

    @Test
    void rejectsMissingAccountAndInvalidMemberHeaders() throws Exception {
        mockMvc.perform(get(PATH).header(MEMBER_HEADER, "999"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get(PATH).header(MEMBER_HEADER, "invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void neverExposesAnotherMembersTransactions() throws Exception {
        long accountOne = createAccount(1L);
        long accountTwo = createAccount(2L);
        LocalDateTime now = LocalDateTime.now();
        long ownId = createDeposit(accountOne, 1L, now);
        createDeposit(accountTwo, 2L, now);
        createDeposit(accountTwo, 2L, now.minusSeconds(1));

        assertThat(ids(getHistory(1L))).containsExactly(ownId);
    }

    @Test
    void transferOutShowsReceiverAccountAndNegativeAmount() throws Exception {
        long fromId = createAccount(1L);
        long toId = createAccount(2L);
        long transferId = createTransfer(fromId, toId);
        createTransaction(fromId, 1L, transferId, "TRANSFER_OUT", 1000, 9000,
                "출금 설명", LocalDateTime.now());

        JsonNode item = getHistory(1L).at("/data/items/0");
        assertThat(item.get("counterpartyAccountNumber").asText()).isEqualTo("200000000002");
        assertThat(item.get("transferNo").asText()).startsWith("TRF-TEST-");
        assertThat(item.get("direction").asText()).isEqualTo("OUT");
        assertThat(item.get("signedAmount").asLong()).isEqualTo(-1000);
    }

    @Test
    void transferInShowsSenderAccountAndPositiveAmount() throws Exception {
        long fromId = createAccount(1L);
        long toId = createAccount(2L);
        long transferId = createTransfer(fromId, toId);
        createTransaction(toId, 2L, transferId, "TRANSFER_IN", 1000, 1000,
                "입금 설명", LocalDateTime.now());

        JsonNode item = getHistory(2L).at("/data/items/0");
        assertThat(item.get("counterpartyAccountNumber").asText()).isEqualTo("200000000001");
        assertThat(item.get("transferNo").asText()).startsWith("TRF-TEST-");
        assertThat(item.get("direction").asText()).isEqualTo("IN");
        assertThat(item.get("signedAmount").asLong()).isEqualTo(1000);
    }

    @Test
    void depositHasNoCounterpartyAndUsesDescriptionAsTitle() throws Exception {
        long accountId = createAccount(1L);
        createTransaction(accountId, 1L, null, "DEPOSIT", 500, 500,
                "직접 설정한 설명", LocalDateTime.now());

        JsonNode item = getHistory(1L).at("/data/items/0");
        assertThat(item.get("counterpartyAccountNumber").isNull()).isTrue();
        assertThat(item.get("transferNo").isNull()).isTrue();
        assertThat(item.get("title").asText()).isEqualTo("직접 설정한 설명");
        assertThat(item.get("direction").asText()).isEqualTo("IN");
        assertThat(item.get("signedAmount").asLong()).isEqualTo(500);
    }

    @Test
    void nullDescriptionsUseTransactionTypeDefaultTitles() throws Exception {
        long fromId = createAccount(1L);
        long toId = createAccount(2L);
        long transferId = createTransfer(fromId, toId);
        LocalDateTime now = LocalDateTime.now();
        createTransaction(fromId, 1L, null, "DEPOSIT", 100, 100, null, now.minusSeconds(3));
        createTransaction(fromId, 1L, transferId, "TRANSFER_IN", 100, 200, null, now.minusSeconds(2));
        createTransaction(fromId, 1L, transferId, "TRANSFER_OUT", 100, 100, null, now.minusSeconds(1));

        JsonNode items = getHistory(1L).at("/data/items");
        assertThat(items.get(0).get("title").asText()).isEqualTo("포인트 송금 출금");
        assertThat(items.get(1).get("title").asText()).isEqualTo("포인트 송금 입금");
        assertThat(items.get(2).get("title").asText()).isEqualTo("개발용 포인트 충전");
    }

    @Test
    void rejectsInvalidDateFormats() throws Exception {
        expectBadRequest("cursorCreatedAt", "invalid-date", "cursorId", "1");
        expectBadRequest("from", "invalid-date");
        expectBadRequest("to", "invalid-date");
    }

    private JsonNode getHistory(Long memberId, String... parameters) throws Exception {
        var request = get(PATH).header(MEMBER_HEADER, memberId.toString());
        for (int index = 0; index < parameters.length; index += 2) {
            request.param(parameters[index], parameters[index + 1]);
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void expectBadRequest(String... parameters) throws Exception {
        ensureAccount(1L);
        var request = get(PATH).header(MEMBER_HEADER, "1");
        for (int index = 0; index < parameters.length; index += 2) {
            request.param(parameters[index], parameters[index + 1]);
        }
        mockMvc.perform(request).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private List<Long> readAllPages(Long memberId, int size) throws Exception {
        List<Long> result = new ArrayList<>();
        String cursorTime = null;
        String cursorId = null;
        boolean hasNext;
        do {
            JsonNode page = cursorTime == null
                    ? getHistory(memberId, "size", String.valueOf(size))
                    : getHistory(memberId, "size", String.valueOf(size),
                    "cursorCreatedAt", cursorTime, "cursorId", cursorId);
            result.addAll(ids(page));
            hasNext = page.at("/data/hasNext").asBoolean();
            cursorTime = hasNext ? page.at("/data/nextCursorCreatedAt").asText() : null;
            cursorId = hasNext ? page.at("/data/nextCursorId").asText() : null;
        } while (hasNext);
        return result;
    }

    private List<Long> ids(JsonNode response) {
        List<Long> result = new ArrayList<>();
        response.at("/data/items").forEach(item -> result.add(item.get("transactionId").asLong()));
        return result;
    }

    private List<String> types(JsonNode response) {
        return values(response, "transactionType");
    }

    private List<String> values(JsonNode response, String field) {
        List<String> result = new ArrayList<>();
        response.at("/data/items").forEach(item -> result.add(item.get(field).asText()));
        return result;
    }

    private List<Long> longValues(JsonNode response, String field) {
        List<Long> result = new ArrayList<>();
        response.at("/data/items").forEach(item -> result.add(item.get(field).asLong()));
        return result;
    }

    private void ensureAccount(Long memberId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE member_id = ?", Integer.class, memberId);
        if (count != null && count == 0) {
            createAccount(memberId);
        }
    }

    private long createAccount(Long memberId) {
        String accountNumber = String.format("200000000%03d", memberId);
        jdbcTemplate.update("""
                INSERT INTO accounts
                    (member_id, account_number, account_password_hash, balance, status)
                VALUES (?, ?, '$2a$10$test', 0, 'ACTIVE')
                """, memberId, accountNumber);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM accounts WHERE member_id = ?", Long.class, memberId);
    }

    private long createDeposit(Long accountId, Long memberId, LocalDateTime createdAt) {
        return createTransaction(accountId, memberId, null, "DEPOSIT", 100, 100,
                "deposit", createdAt);
    }

    private long createTransfer(Long fromAccountId, Long toAccountId) {
        Long fromMemberId = jdbcTemplate.queryForObject(
                "SELECT member_id FROM accounts WHERE id = ?", Long.class, fromAccountId);
        Long toMemberId = jdbcTemplate.queryForObject(
                "SELECT member_id FROM accounts WHERE id = ?", Long.class, toAccountId);
        String transferNo = "TRF-TEST-" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO transfers
                    (transfer_no, from_account_id, to_account_id, from_member_id,
                     to_member_id, amount, status, completed_at)
                VALUES (?, ?, ?, ?, ?, 1000, 'COMPLETED', CURRENT_TIMESTAMP(6))
                """, transferNo, fromAccountId, toAccountId, fromMemberId, toMemberId);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM transfers WHERE transfer_no = ?", Long.class, transferNo);
    }

    private long createTransaction(
            Long accountId, Long memberId, Long transferId, String type,
            long amount, long balanceAfter, String description, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO account_transactions
                    (account_id, member_id, transfer_id, transaction_type,
                     amount, balance_after, description, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, accountId, memberId, transferId, type, amount, balanceAfter,
                description, Timestamp.valueOf(createdAt));
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM account_transactions WHERE account_id = ?", Long.class, accountId);
    }
}
