package com.pointbank.banking.account.dto;

import com.pointbank.banking.transaction.domain.AccountTransactionType;

public record AccountDepositResponse(
        Long accountId,
        String accountNumber,
        long amount,
        long balanceAfter,
        AccountTransactionType transactionType
) {
}
