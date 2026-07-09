package com.pointbank.ledger.api.dto;

import java.time.LocalDateTime;

public record SecuritiesCashDepositResponse(
        Long fundTransferId,
        String requestNo,
        Long memberId,
        Long bankingAccountId,
        Long securitiesCashAccountId,
        long amount,
        long bankingBalanceAfter,
        long cashBalance,
        long reservedCash,
        long availableCash,
        String status,
        LocalDateTime completedAt
) {
}
