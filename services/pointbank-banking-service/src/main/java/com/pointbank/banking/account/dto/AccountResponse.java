package com.pointbank.banking.account.dto;

public record AccountResponse(
        Long accountId,
        String accountNumber,
        long balance,
        String status
) {
}
