package com.pointbank.ledger.api.controller;

import com.pointbank.ledger.api.dto.SecuritiesBuyDebitRequest;
import com.pointbank.ledger.api.dto.SecuritiesSellCreditRequest;
import com.pointbank.ledger.api.dto.SecuritiesTradeFundsResponse;
import com.pointbank.ledger.api.service.SecuritiesTradeLedgerService;
import com.pointbank.ledger.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ledger/securities/trades")
@RequiredArgsConstructor
public class SecuritiesTradeLedgerController {
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final SecuritiesTradeLedgerService securitiesTradeLedgerService;

    @PostMapping("/buy/debit")
    public ApiResponse<SecuritiesTradeFundsResponse> debitBuyFunds(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody SecuritiesBuyDebitRequest request
    ) {
        return ApiResponse.success(
                "Securities buy funds debited.",
                securitiesTradeLedgerService.debitBuyFunds(idempotencyKey, request));
    }

    @PostMapping("/sell/credit")
    public ApiResponse<SecuritiesTradeFundsResponse> creditSellFunds(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody SecuritiesSellCreditRequest request
    ) {
        return ApiResponse.success(
                "Securities sell funds credited.",
                securitiesTradeLedgerService.creditSellFunds(idempotencyKey, request));
    }
}
