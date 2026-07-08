package com.pointbank.securities.account.dto;

import com.pointbank.securities.account.domain.SecuritiesAccount;
import com.pointbank.securities.account.domain.SecuritiesAccountStatus;

public record SecuritiesAccountResponse(
        Long accountId,
        String accountNumber,
        SecuritiesAccountStatus status
) {

    public static SecuritiesAccountResponse from(SecuritiesAccount account) {
        return new SecuritiesAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getStatus()
        );
    }
}
