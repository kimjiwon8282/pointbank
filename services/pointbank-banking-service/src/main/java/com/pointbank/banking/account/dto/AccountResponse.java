package com.pointbank.banking.account.dto;

import com.pointbank.banking.account.domain.Account;
import com.pointbank.banking.account.domain.AccountStatus;

public record AccountResponse(
        Long accountId,
        String accountNumber,
        long balance,
        AccountStatus status
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getStatus()
        );
    }
}
