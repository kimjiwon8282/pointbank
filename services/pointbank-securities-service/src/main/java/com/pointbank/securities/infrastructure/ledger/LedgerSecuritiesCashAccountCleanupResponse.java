package com.pointbank.securities.infrastructure.ledger;

public record LedgerSecuritiesCashAccountCleanupResponse(
        Long memberId,
        boolean deleted,
        String message
) {
}
