package com.pointbank.ledger.api.dto;

public record SellOrderReversalResponse(
        String reversalRequestNo,
        String originalLedgerRequestNo,
        Long memberId,
        String stockCode,
        long reversalAmount,
        long balanceAfter,
        String status
) {
}
