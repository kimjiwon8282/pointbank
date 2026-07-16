package com.pointbank.securities.infrastructure.ledger;

public record LedgerBuyReversalResponse(
        String reversalRequestNo,
        String originalLedgerRequestNo,
        Long memberId,
        String stockCode,
        long reversalAmount,
        long balanceAfter,
        String status
) {
}
