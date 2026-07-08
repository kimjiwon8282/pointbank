package com.pointbank.banking.funds.dto;

import com.pointbank.banking.funds.domain.SecuritiesCashAccount;
import com.pointbank.banking.funds.domain.SecuritiesCashAccountStatus;

public record SecuritiesCashAccountResponse(
        Long securitiesCashAccountId,
        Long memberId,
        long cashBalance,
        long reservedCash,
        long availableCash,
        SecuritiesCashAccountStatus status
) {

    public static SecuritiesCashAccountResponse from(SecuritiesCashAccount account) {
        return new SecuritiesCashAccountResponse(
                account.getId(),
                account.getMemberId(),
                account.getCashBalance(),
                account.getReservedCash(),
                account.getCashBalance() - account.getReservedCash(),
                account.getStatus()
        );
    }
}
