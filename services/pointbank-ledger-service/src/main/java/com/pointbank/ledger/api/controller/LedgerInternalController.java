package com.pointbank.ledger.api.controller;

import com.pointbank.ledger.api.dto.AccountDepositRequest;
import com.pointbank.ledger.api.dto.AccountDepositResponse;
import com.pointbank.ledger.api.dto.BankingAccountCreateRequest;
import com.pointbank.ledger.api.dto.BankingAccountResponse;
import com.pointbank.ledger.api.dto.SecuritiesCashAccountCleanupResponse;
import com.pointbank.ledger.api.dto.SecuritiesCashAccountCreateRequest;
import com.pointbank.ledger.api.dto.SecuritiesCashAccountResponse;
import com.pointbank.ledger.api.dto.SecuritiesCashDepositRequest;
import com.pointbank.ledger.api.dto.SecuritiesCashDepositResponse;
import com.pointbank.ledger.api.dto.TransactionHistoryRequest;
import com.pointbank.ledger.api.dto.TransactionHistorySliceResponse;
import com.pointbank.ledger.api.dto.TransferCreateRequest;
import com.pointbank.ledger.api.dto.TransferResponse;
import com.pointbank.ledger.api.service.LedgerInternalService;
import com.pointbank.ledger.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/internal/ledger")
@RequiredArgsConstructor
public class LedgerInternalController {
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final LedgerInternalService ledgerInternalService;

    @PostMapping("/banking/accounts")
    public ApiResponse<BankingAccountResponse> createBankingAccount(
            @Valid @RequestBody BankingAccountCreateRequest request
    ) {
        return ApiResponse.success("계좌 개설에 성공했습니다.", ledgerInternalService.createBankingAccount(request));
    }

    @GetMapping("/banking/accounts/me")
    public ApiResponse<BankingAccountResponse> getBankingAccount(@RequestParam Long memberId) {
        return ApiResponse.success("내 계좌 조회에 성공했습니다.", ledgerInternalService.getBankingAccount(memberId));
    }

    @PostMapping("/banking/accounts/deposit")
    public ApiResponse<AccountDepositResponse> deposit(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody AccountDepositRequest request
    ) {
        return ApiResponse.success(
                "개발용 포인트 충전이 완료되었습니다.",
                ledgerInternalService.deposit(request.memberId(), idempotencyKey, request.amount()));
    }

    @PostMapping("/banking/transfers")
    public ApiResponse<TransferResponse> transfer(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody TransferCreateRequest request
    ) {
        return ApiResponse.success(
                "송금이 완료되었습니다.",
                ledgerInternalService.transfer(
                        request.memberId(), idempotencyKey,
                        request.toAccountNumber(), request.amount(), request.accountPassword()));
    }

    @GetMapping("/banking/transactions")
    public ApiResponse<TransactionHistorySliceResponse> getHistories(
            @RequestParam Long memberId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) Integer size
    ) {
        TransactionHistoryRequest request = new TransactionHistoryRequest(
                memberId, type, from, to, cursorCreatedAt, cursorId, size);
        return ApiResponse.success("거래내역 조회에 성공했습니다.", ledgerInternalService.getBankingHistories(request));
    }

    @PostMapping("/securities/cash/accounts")
    public ApiResponse<SecuritiesCashAccountResponse> createSecuritiesCashAccount(
            @Valid @RequestBody SecuritiesCashAccountCreateRequest request
    ) {
        return ApiResponse.success(
                "증권 예수금 계좌 생성에 성공했습니다.",
                ledgerInternalService.createSecuritiesCashAccount(request.memberId()));
    }

    @GetMapping("/securities/cash/me")
    public ApiResponse<SecuritiesCashAccountResponse> getSecuritiesCashAccount(@RequestParam Long memberId) {
        return ApiResponse.success(
                "증권 예수금 조회에 성공했습니다.",
                ledgerInternalService.getSecuritiesCashAccount(memberId));
    }

    @DeleteMapping("/securities/cash/accounts")
    public ApiResponse<SecuritiesCashAccountCleanupResponse> cleanupSecuritiesCashAccount(@RequestParam Long memberId) {
        return ApiResponse.success(
                "SECURITIES_CASH cleanup completed.",
                ledgerInternalService.cleanupSecuritiesCashAccount(memberId));
    }

    @PostMapping("/securities/cash/deposit")
    public ApiResponse<SecuritiesCashDepositResponse> depositSecuritiesCash(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody SecuritiesCashDepositRequest request
    ) {
        return ApiResponse.success(
                "증권 예수금 충전에 성공했습니다.",
                ledgerInternalService.depositSecuritiesCash(
                        request.memberId(), idempotencyKey, request.amount(), request.accountPassword()));
    }
}
