package com.pointbank.ledger.api.dto;

public record BuyOrderReversalResponse(
        String reversalRequestNo,
        String originalLedgerRequestNo,
        Long memberId,
        String stockCode,
        long reversalAmount,
        long balanceAfter,
        String status
) {
}
