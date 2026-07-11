package com.pointbank.ledger.api.dto;

public record SecuritiesCashAccountCleanupResponse(
        Long memberId,
        boolean deleted,
        String message
) {
}
