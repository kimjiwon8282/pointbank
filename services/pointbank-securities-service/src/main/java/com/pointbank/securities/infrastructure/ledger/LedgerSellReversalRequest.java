package com.pointbank.securities.infrastructure.ledger;

public record LedgerSellReversalRequest(
        Long memberId,
        String orderNo,
        String stockCode,
        long reversalAmount,
        String originalLedgerRequestNo,
        String reasonCode,
        String reasonMessage
) {
}
