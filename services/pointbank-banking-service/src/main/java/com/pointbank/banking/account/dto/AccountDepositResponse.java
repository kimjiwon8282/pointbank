package com.pointbank.banking.account.dto;

public record AccountDepositResponse(
        Long accountId,
        String accountNumber,
        long amount,
        long balanceAfter,
        String transactionType
) {
}
