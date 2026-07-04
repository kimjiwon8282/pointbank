package com.pointbank.banking.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankingAuthIntegrationTest {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String ROLE_HEADER = "X-Role";

    @Autowired
    MockMvc mockMvc;

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
}
