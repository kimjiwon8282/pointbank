package com.pointbank.banking.account.controller;

import com.pointbank.banking.account.dto.AccountCreateRequest;
import com.pointbank.banking.account.dto.AccountDepositRequest;
import com.pointbank.banking.account.dto.AccountDepositResponse;
import com.pointbank.banking.account.dto.AccountResponse;
import com.pointbank.banking.account.service.AccountService;
import com.pointbank.banking.auth.support.CurrentMemberHeaderResolver;
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
@RequestMapping("/api/banking/accounts")
@RequiredArgsConstructor
public class AccountController {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final AccountService accountService;
    private final CurrentMemberHeaderResolver currentMemberHeaderResolver;

    @PostMapping
    public ApiResponse<AccountResponse> create(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader,
            @Valid @RequestBody AccountCreateRequest request
    ) {
        Long memberId = currentMemberHeaderResolver.resolveMemberId(memberIdHeader);
        return ApiResponse.success("계좌 개설에 성공했습니다.", accountService.create(memberId, request));
    }

    @GetMapping("/me")
    public ApiResponse<AccountResponse> getMine(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader
    ) {
        Long memberId = currentMemberHeaderResolver.resolveMemberId(memberIdHeader);
        return ApiResponse.success("내 계좌 조회에 성공했습니다.", accountService.getMine(memberId));
    }

    @PostMapping("/deposit")
    public ApiResponse<AccountDepositResponse> deposit(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody AccountDepositRequest request
    ) {
        Long memberId = currentMemberHeaderResolver.resolveMemberId(memberIdHeader);
        return ApiResponse.success(
                "개발용 포인트 충전이 완료되었습니다.",
                accountService.deposit(memberId, idempotencyKey, request)
        );
    }
}
