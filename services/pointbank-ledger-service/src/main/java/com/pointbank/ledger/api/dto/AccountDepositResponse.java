package com.pointbank.ledger.api.dto;

import java.time.LocalDateTime;

public record AccountDepositResponse(
        String requestNo,
        Long memberId,
        Long accountId,
        long amount,
        long balanceAfter,
        String status,
        LocalDateTime completedAt
) {
}
