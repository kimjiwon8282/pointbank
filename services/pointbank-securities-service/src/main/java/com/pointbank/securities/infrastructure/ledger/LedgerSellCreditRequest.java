package com.pointbank.securities.infrastructure.ledger;

public record LedgerSellCreditRequest(
        Long memberId,
        String orderNo,
        String stockCode,
        long sellAmount,
        long fee,
        long tax,
        long netAmount
) {
}
