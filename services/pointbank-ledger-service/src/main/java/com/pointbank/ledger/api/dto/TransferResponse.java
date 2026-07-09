package com.pointbank.ledger.api.dto;

import java.time.LocalDateTime;

public record TransferResponse(
        Long transferRequestId,
        String requestNo,
        String transferNo,
        Long fromAccountId,
        Long toAccountId,
        String fromAccountNumber,
        String toAccountNumber,
        long amount,
        long fromBalanceAfter,
        long toBalanceAfter,
        String status,
        LocalDateTime completedAt
) {
}
