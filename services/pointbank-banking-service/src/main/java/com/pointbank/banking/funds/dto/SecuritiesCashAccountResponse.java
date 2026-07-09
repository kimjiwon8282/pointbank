package com.pointbank.banking.funds.dto;

public record SecuritiesCashAccountResponse(
        Long securitiesCashAccountId,
        Long memberId,
        long cashBalance,
        long reservedCash,
        long availableCash,
        String status
) {
}
