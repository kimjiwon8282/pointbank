package com.pointbank.ledger.api.dto;

public record SecuritiesTradeFundsResponse(
        String requestNo,
        Long memberId,
        Long accountId,
        String transferType,
        String entryType,
        String stockCode,
        long amount,
        long balanceAfter,
        String status
) {
}
