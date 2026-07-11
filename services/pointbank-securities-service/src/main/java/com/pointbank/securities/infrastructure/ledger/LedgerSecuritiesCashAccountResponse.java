package com.pointbank.securities.infrastructure.ledger;

public record LedgerSecuritiesCashAccountResponse(
        Long securitiesCashAccountId,
        Long memberId,
        long cashBalance,
        long reservedCash,
        long availableCash,
        String status
) {
}
