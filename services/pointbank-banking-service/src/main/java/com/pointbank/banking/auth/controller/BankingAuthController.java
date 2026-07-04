package com.pointbank.banking.auth.controller;

import com.pointbank.banking.auth.dto.BankingMeResponse;
import com.pointbank.banking.auth.support.CurrentMemberHeaderResolver;
import com.pointbank.banking.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banking")
@RequiredArgsConstructor
public class BankingAuthController {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String ROLE_HEADER = "X-Role";

    private final CurrentMemberHeaderResolver currentMemberHeaderResolver;

    @GetMapping("/me")
    public ApiResponse<BankingMeResponse> getMe(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader,
            @RequestHeader(value = ROLE_HEADER, required = false) String roleHeader
    ) {
        BankingMeResponse response = currentMemberHeaderResolver.resolve(memberIdHeader, roleHeader);
        return ApiResponse.success("뱅킹 인증 정보 조회에 성공했습니다.", response);
    }
}
