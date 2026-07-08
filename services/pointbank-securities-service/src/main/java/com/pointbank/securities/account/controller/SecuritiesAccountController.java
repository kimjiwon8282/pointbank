package com.pointbank.securities.account.controller;

import com.pointbank.securities.account.dto.SecuritiesAccountCreateRequest;
import com.pointbank.securities.account.dto.SecuritiesAccountResponse;
import com.pointbank.securities.account.service.SecuritiesAccountService;
import com.pointbank.securities.auth.support.CurrentMemberHeaderResolver;
import com.pointbank.securities.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/securities/accounts")
@RequiredArgsConstructor
public class SecuritiesAccountController {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    private final SecuritiesAccountService securitiesAccountService;
    private final CurrentMemberHeaderResolver currentMemberHeaderResolver;

    @PostMapping
    public ApiResponse<SecuritiesAccountResponse> create(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader,
            @Valid @RequestBody SecuritiesAccountCreateRequest request
    ) {
        Long memberId = currentMemberHeaderResolver.resolveMemberId(memberIdHeader);
        return ApiResponse.success("증권계좌 개설에 성공했습니다.", securitiesAccountService.create(memberId, request));
    }

    @GetMapping("/me")
    public ApiResponse<SecuritiesAccountResponse> getMine(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader
    ) {
        Long memberId = currentMemberHeaderResolver.resolveMemberId(memberIdHeader);
        return ApiResponse.success("내 증권계좌 조회에 성공했습니다.", securitiesAccountService.getMine(memberId));
    }
}
