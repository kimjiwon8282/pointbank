package com.pointbank.ledger.api.dto;

import java.time.LocalDateTime;

public record BankingAccountResponse(
        Long accountId,
        Long memberId,
        String accountNumber,
        long balance,
        String status,
        LocalDateTime createdAt
) {
}
