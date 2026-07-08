package com.pointbank.banking.funds.controller;

import com.pointbank.banking.auth.support.CurrentMemberHeaderResolver;
import com.pointbank.banking.funds.dto.SecuritiesCashAccountResponse;
import com.pointbank.banking.funds.dto.SecuritiesDepositRequest;
import com.pointbank.banking.funds.dto.SecuritiesDepositResponse;
import com.pointbank.banking.funds.service.SecuritiesFundsService;
import com.pointbank.banking.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banking/funds/securities")
@RequiredArgsConstructor
public class SecuritiesFundsController {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final CurrentMemberHeaderResolver currentMemberHeaderResolver;
    private final SecuritiesFundsService securitiesFundsService;

    @PostMapping("/deposit")
    public ApiResponse<SecuritiesDepositResponse> deposit(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody SecuritiesDepositRequest request
    ) {
        Long memberId = currentMemberHeaderResolver.resolveMemberId(memberIdHeader);
        return ApiResponse.success(
                "증권 예수금 충전에 성공했습니다.",
                securitiesFundsService.deposit(memberId, idempotencyKey, request)
        );
    }

    @GetMapping("/me")
    public ApiResponse<SecuritiesCashAccountResponse> getMine(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader
    ) {
        Long memberId = currentMemberHeaderResolver.resolveMemberId(memberIdHeader);
        return ApiResponse.success(
                "내 증권 예수금 조회에 성공했습니다.",
                securitiesFundsService.getMine(memberId)
        );
    }
}
