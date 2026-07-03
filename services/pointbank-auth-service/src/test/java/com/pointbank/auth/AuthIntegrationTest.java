package com.pointbank.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    private static final String PASSWORD = "Password1!";
    private static final String SIMPLE_PASSWORD = "123456";
    private static final String WRONG_SIMPLE_PASSWORD = "654321";
    private static final String DEVICE_ID = "test-device-001";
    private static final AtomicInteger PHONE_SEQUENCE = new AtomicInteger();

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("auth_db")
            .withUsername("pointbank")
            .withPassword("pointbank")
            .withInitScript("schema-auth-test.sql");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired TestRestTemplate restTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM member_devices");
        jdbcTemplate.update("DELETE FROM phone_verifications");
        jdbcTemplate.update("DELETE FROM members");
    }

    @Test
    void 회원가입_성공_시_비밀번호는_해시로_저장되고_간편_비밀번호는_미설정이다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();

        // when
        JsonNode signup = signup(phoneNumber);

        // then
        assertThat(signup.path("success").asBoolean()).isTrue();
        Map<String, Object> member = memberByPhone(phoneNumber);
        assertThat(member.get("password_hash")).isNotNull().isNotEqualTo(PASSWORD);
        assertThat(member.get("password_hash").toString()).isNotBlank();
        assertThat(member.get("role")).isEqualTo("USER");
        assertThat(member.get("status")).isEqualTo("ACTIVE");
        assertThat(asBoolean(member.get("simple_password_set"))).isFalse();
    }

    @Test
    void 일반_로그인_성공_시_토큰을_발급하고_Refresh_Token은_해시로_저장한다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);

        // when
        Tokens tokens = login(phoneNumber, DEVICE_ID);

        // then
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        Map<String, Object> stored = singleRow("SELECT * FROM refresh_tokens");
        assertThat(stored.get("status")).isEqualTo("ACTIVE");
        assertThat(stored.get("device_id")).isEqualTo(DEVICE_ID);
        assertThat(stored.get("token_hash").toString()).isNotBlank().isNotEqualTo(tokens.refreshToken());
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE token_hash = ?", tokens.refreshToken())).isZero();
    }

    @Test
    void Access_Token으로_보호_API에_접근하고_토큰이_없으면_거부된다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);
        Tokens tokens = login(phoneNumber, DEVICE_ID);

        // when
        ResponseEntity<String> authenticated = get("/api/auth/me", bearer(tokens.accessToken()));
        ResponseEntity<String> anonymous = get("/api/auth/me", new HttpHeaders());

        // then
        assertThat(authenticated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(authenticated).path("data").path("phoneNumber").asText()).isEqualTo(phoneNumber);
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void Access_Token이_유효해도_회원이_ACTIVE가_아니면_인증에_실패한다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);
        Tokens tokens = login(phoneNumber, DEVICE_ID);
        jdbcTemplate.update("UPDATE members SET status = 'LOCKED' WHERE phone_number = ?", phoneNumber);

        // when
        ResponseEntity<String> response = get("/api/auth/me", bearer(tokens.accessToken()));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(json(response).path("code").asText()).isEqualTo("MEMBER_NOT_ACTIVE");
    }

    @Test
    void 간편_비밀번호_설정_시_BCrypt_해시와_설정_상태가_저장된다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);
        Tokens tokens = login(phoneNumber, DEVICE_ID);

        // when
        ResponseEntity<String> response = post("/api/auth/simple-password",
                Map.of("deviceId", DEVICE_ID, "simplePassword", SIMPLE_PASSWORD,
                        "confirmSimplePassword", SIMPLE_PASSWORD), bearer(tokens.accessToken()));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(response).path("success").asBoolean()).isTrue();
        assertThat(asBoolean(memberByPhone(phoneNumber).get("simple_password_set"))).isTrue();
        Map<String, Object> device = singleRow("SELECT * FROM member_devices");
        assertThat(device.get("device_id")).isEqualTo(DEVICE_ID);
        assertThat(device.get("simple_password_hash").toString()).isNotBlank().isNotEqualTo(SIMPLE_PASSWORD)
                .startsWith("$2");
        assertThat(((Number) device.get("failed_count")).intValue()).isZero();
        assertThat(device.get("locked_until")).isNull();
    }

    @Test
    void 간편_로그인_성공_시_토큰을_재발급하고_기존_ACTIVE_토큰을_폐기한다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);
        Tokens oldTokens = login(phoneNumber, DEVICE_ID);
        setupSimplePassword(oldTokens.accessToken());

        // when
        ResponseEntity<String> response = post("/api/auth/simple-login",
                Map.of("deviceId", DEVICE_ID, "simplePassword", SIMPLE_PASSWORD), new HttpHeaders());
        JsonNode body = json(response);

        // then
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("accessToken").asText()).isNotBlank();
        assertThat(body.path("data").path("refreshToken").asText()).isNotBlank().isNotEqualTo(oldTokens.refreshToken());
        assertThat(body.path("data").path("simplePasswordSet").asBoolean()).isTrue();
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE status = 'REVOKED'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE device_id = ? AND status = 'ACTIVE'", DEVICE_ID)).isEqualTo(1);
    }

    @Test
    void 간편_비밀번호_5회_실패_시_기기가_잠긴다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);
        Tokens tokens = login(phoneNumber, DEVICE_ID);
        setupSimplePassword(tokens.accessToken());

        // when
        for (int attempt = 0; attempt < 5; attempt++) {
            ResponseEntity<String> failure = simpleLogin(WRONG_SIMPLE_PASSWORD);
            assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // then
        Map<String, Object> device = singleRow("SELECT * FROM member_devices");
        assertThat(((Number) device.get("failed_count")).intValue()).isGreaterThanOrEqualTo(5);
        assertThat(device.get("locked_until")).isNotNull();
        ResponseEntity<String> locked = simpleLogin(SIMPLE_PASSWORD);
        assertThat(locked.getStatusCode().value()).isEqualTo(423);
        assertThat(json(locked).path("code").asText()).isEqualTo("SIMPLE_PASSWORD_LOCKED");
    }

    @Test
    void Refresh_Token_재발급은_Rotation되고_기존_토큰을_재사용할_수_없다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);
        Tokens oldTokens = login(phoneNumber, DEVICE_ID);

        // when
        ResponseEntity<String> first = refresh(oldTokens.refreshToken());
        JsonNode firstBody = json(first);
        String newRefreshToken = firstBody.path("data").path("refreshToken").asText();
        ResponseEntity<String> reused = refresh(oldTokens.refreshToken());

        // then
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstBody.path("data").path("accessToken").asText()).isNotBlank();
        assertThat(newRefreshToken).isNotBlank().isNotEqualTo(oldTokens.refreshToken());
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE status = 'REVOKED'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE status = 'ACTIVE'")).isEqualTo(1);
        assertThat(reused.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(json(reused).path("code").asText()).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void 로그아웃은_Refresh_Token을_폐기하고_중복_호출되어도_성공한다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);
        Tokens tokens = login(phoneNumber, DEVICE_ID);
        Map<String, String> request = Map.of("refreshToken", tokens.refreshToken());
        HttpHeaders authorization = bearer(tokens.accessToken());

        // when
        ResponseEntity<String> first = post("/api/auth/logout", request, authorization);
        ResponseEntity<String> second = post("/api/auth/logout", request, authorization);
        ResponseEntity<String> refresh = refresh(tokens.refreshToken());

        // then
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE status = 'REVOKED'")).isEqualTo(1);
        assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(json(refresh).path("code").asText()).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void 본인확인_없이_회원가입하면_실패한다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();

        // when
        ResponseEntity<String> response = signupRequest(phoneNumber, PASSWORD);

        // then
        assertErrorCode(response, "PHONE_VERIFICATION_NOT_FOUND");
        assertThat(count("SELECT COUNT(*) FROM members WHERE phone_number = ?", phoneNumber)).isZero();
    }

    @Test
    void 인증번호_확인_없이_회원가입하면_실패한다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        assertSuccess(post("/api/auth/phone-verifications",
                Map.of("phoneNumber", phoneNumber), new HttpHeaders()));

        // when
        ResponseEntity<String> response = signupRequest(phoneNumber, PASSWORD);

        // then
        assertErrorCode(response, "PHONE_VERIFICATION_NOT_COMPLETED");
        assertThat(count("SELECT COUNT(*) FROM members WHERE phone_number = ?", phoneNumber)).isZero();
    }

    @Test
    void 동일_휴대폰_번호로_중복_회원가입하면_실패한다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);

        // when
        ResponseEntity<String> response = signupRequest(phoneNumber, PASSWORD);

        // then
        assertErrorCode(response, "DUPLICATE_PHONE_NUMBER");
        assertThat(count("SELECT COUNT(*) FROM members WHERE phone_number = ?", phoneNumber)).isEqualTo(1);
    }

    @Test
    void 일반_로그인_비밀번호가_틀리면_실패하고_Refresh_Token이_생성되지_않는다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);

        // when
        ResponseEntity<String> response = post("/api/auth/login",
                Map.of("phoneNumber", phoneNumber, "password", "WrongPassword1!", "deviceId", DEVICE_ID),
                new HttpHeaders());

        // then
        assertErrorCode(response, "INVALID_PASSWORD");
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens")).isZero();
    }

    @Test
    void Refresh_Token_재발급_API에_Access_Token을_보내면_실패한다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);
        Tokens tokens = login(phoneNumber, DEVICE_ID);

        // when
        ResponseEntity<String> response = refresh(tokens.accessToken());

        // then
        assertErrorCode(response, "INVALID_TOKEN");
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE status = 'ACTIVE'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens")).isEqualTo(1);
    }

    @Test
    void 간편_비밀번호_확인값이_다르면_설정에_실패하고_기기_정보가_저장되지_않는다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);
        Tokens tokens = login(phoneNumber, DEVICE_ID);

        // when
        ResponseEntity<String> response = post("/api/auth/simple-password",
                Map.of("deviceId", DEVICE_ID, "simplePassword", SIMPLE_PASSWORD,
                        "confirmSimplePassword", "111111"), bearer(tokens.accessToken()));

        // then
        assertErrorCode(response, "INVALID_SIMPLE_PASSWORD");
        assertThat(asBoolean(memberByPhone(phoneNumber).get("simple_password_set"))).isFalse();
        assertThat(count("SELECT COUNT(*) FROM member_devices WHERE device_id = ?", DEVICE_ID)).isZero();
    }

    @Test
    void 간편_비밀번호_설정_전_간편_로그인은_실패한다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);
        login(phoneNumber, DEVICE_ID);

        // when
        ResponseEntity<String> response = simpleLogin(SIMPLE_PASSWORD);

        // then
        assertErrorCode(response, "SIMPLE_PASSWORD_NOT_SET");
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE status = 'ACTIVE'")).isEqualTo(1);
    }

    @Test
    void 다른_회원의_Refresh_Token으로_로그아웃하면_실패한다() throws Exception {
        // given
        String phoneNumberA = uniquePhoneNumber();
        String phoneNumberB = uniquePhoneNumber();
        signup(phoneNumberA);
        Tokens tokensA = login(phoneNumberA, "test-device-A01");
        signup(phoneNumberB);
        Tokens tokensB = login(phoneNumberB, "test-device-B01");

        // when
        ResponseEntity<String> response = post("/api/auth/logout",
                Map.of("refreshToken", tokensB.refreshToken()), bearer(tokensA.accessToken()));

        // then
        assertErrorCode(response, "INVALID_REFRESH_TOKEN");
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE status = 'ACTIVE'")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE status = 'REVOKED'")).isZero();
    }

    @Test
    void 회원_상태가_ACTIVE가_아니면_Refresh_Token_재발급도_실패한다() throws Exception {
        // given
        String phoneNumber = uniquePhoneNumber();
        signup(phoneNumber);
        Tokens tokens = login(phoneNumber, DEVICE_ID);
        jdbcTemplate.update("UPDATE members SET status = 'LOCKED' WHERE phone_number = ?", phoneNumber);

        // when
        ResponseEntity<String> response = refresh(tokens.refreshToken());

        // then
        assertErrorCode(response, "MEMBER_NOT_ACTIVE");
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE status = 'ACTIVE'")).isEqualTo(1);
    }

    private JsonNode signup(String phoneNumber) throws Exception {
        assertSuccess(post("/api/auth/phone-verifications", Map.of("phoneNumber", phoneNumber), new HttpHeaders()));
        assertSuccess(post("/api/auth/phone-verifications/confirm",
                Map.of("phoneNumber", phoneNumber, "verificationCode", "123456"), new HttpHeaders()));
        ResponseEntity<String> response = post("/api/auth/signup",
                Map.of("name", "테스트회원", "phoneNumber", phoneNumber, "password", PASSWORD), new HttpHeaders());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return json(response);
    }

    private ResponseEntity<String> signupRequest(String phoneNumber, String password) {
        return post("/api/auth/signup",
                Map.of("name", "테스트회원", "phoneNumber", phoneNumber, "password", password),
                new HttpHeaders());
    }

    private Tokens login(String phoneNumber, String deviceId) throws Exception {
        ResponseEntity<String> response = post("/api/auth/login",
                Map.of("phoneNumber", phoneNumber, "password", PASSWORD, "deviceId", deviceId), new HttpHeaders());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = json(response).path("data");
        return new Tokens(data.path("accessToken").asText(), data.path("refreshToken").asText());
    }

    private void setupSimplePassword(String accessToken) throws Exception {
        assertSuccess(post("/api/auth/simple-password",
                Map.of("deviceId", DEVICE_ID, "simplePassword", SIMPLE_PASSWORD,
                        "confirmSimplePassword", SIMPLE_PASSWORD), bearer(accessToken)));
    }

    private ResponseEntity<String> simpleLogin(String password) {
        return post("/api/auth/simple-login",
                Map.of("deviceId", DEVICE_ID, "simplePassword", password), new HttpHeaders());
    }

    private ResponseEntity<String> refresh(String refreshToken) {
        return post("/api/auth/token/refresh", Map.of("refreshToken", refreshToken), new HttpHeaders());
    }

    private ResponseEntity<String> post(String path, Object body, HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> get(String path, HttpHeaders headers) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private HttpHeaders bearer(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private void assertSuccess(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(response).path("success").asBoolean()).isTrue();
    }

    private void assertErrorCode(ResponseEntity<String> response, String expectedCode) throws Exception {
        JsonNode body = json(response);
        assertThat(response.getStatusCode().isError()).isTrue();
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asText()).isEqualTo(expectedCode);
    }

    private Map<String, Object> memberByPhone(String phoneNumber) {
        return jdbcTemplate.queryForMap("SELECT * FROM members WHERE phone_number = ?", phoneNumber);
    }

    private Map<String, Object> singleRow(String sql) {
        return jdbcTemplate.queryForMap(sql);
    }

    private int count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }

    private boolean asBoolean(Object value) {
        return value instanceof Boolean booleanValue ? booleanValue : ((Number) value).intValue() != 0;
    }

    private String uniquePhoneNumber() {
        int suffix = Math.floorMod((int) System.nanoTime() + PHONE_SEQUENCE.incrementAndGet(), 100_000_000);
        return "010" + String.format("%08d", suffix);
    }

    private record Tokens(String accessToken, String refreshToken) { }
}
