package com.pointbank.securities.infrastructure.ledger;

public record LedgerBuyDebitRequest(
        Long memberId,
        String orderNo,
        String stockCode,
        long orderAmount,
        long fee,
        long totalAmount
) {
}
