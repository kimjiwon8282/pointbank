package com.pointbank.securities.infrastructure.ledger;

public record LedgerBuyReversalRequest(
        Long memberId,
        String orderNo,
        String stockCode,
        long reversalAmount,
        String originalLedgerRequestNo,
        String reasonCode,
        String reasonMessage
) {
}
