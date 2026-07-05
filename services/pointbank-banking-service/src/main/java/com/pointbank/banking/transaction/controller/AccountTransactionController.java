package com.pointbank.banking.transaction.controller;

import com.pointbank.banking.auth.support.CurrentMemberHeaderResolver;
import com.pointbank.banking.global.response.ApiResponse;
import com.pointbank.banking.transaction.dto.TransactionHistoryRequest;
import com.pointbank.banking.transaction.dto.TransactionHistorySliceResponse;
import com.pointbank.banking.transaction.service.AccountTransactionQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banking/transactions")
@RequiredArgsConstructor
public class AccountTransactionController {
    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    private final AccountTransactionQueryService accountTransactionQueryService;
    private final CurrentMemberHeaderResolver currentMemberHeaderResolver;

    @GetMapping
    public ApiResponse<TransactionHistorySliceResponse> getHistories(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader,
            @Valid @ModelAttribute TransactionHistoryRequest request
    ) {
        Long memberId = currentMemberHeaderResolver.resolveMemberId(memberIdHeader);
        return ApiResponse.success(
                "거래내역 조회에 성공했습니다.",
                accountTransactionQueryService.getHistories(memberId, request));
    }
}
