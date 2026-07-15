package com.pointbank.securities.infrastructure.ledger;

public record LedgerTradeFundsResponse(
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
