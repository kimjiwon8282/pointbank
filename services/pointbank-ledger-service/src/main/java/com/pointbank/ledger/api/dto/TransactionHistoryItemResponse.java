package com.pointbank.ledger.api.dto;

import java.time.LocalDateTime;

public record TransactionHistoryItemResponse(
        Long transactionId,
        String transactionType,
        String direction,
        String title,
        long amount,
        long signedAmount,
        long balanceAfter,
        String counterpartyAccountNumber,
        String transferNo,
        LocalDateTime createdAt
) {
}
