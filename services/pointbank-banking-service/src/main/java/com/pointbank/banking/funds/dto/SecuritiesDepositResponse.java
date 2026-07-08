package com.pointbank.banking.funds.dto;

import com.pointbank.banking.funds.domain.FundTransferRequest;
import com.pointbank.banking.funds.domain.FundTransferStatus;

import java.time.LocalDateTime;

public record SecuritiesDepositResponse(
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
        FundTransferStatus status,
        LocalDateTime completedAt
) {

    public static SecuritiesDepositResponse of(
            FundTransferRequest transferRequest,
            long bankingBalanceAfter,
            long cashBalance,
            long reservedCash
    ) {
        return new SecuritiesDepositResponse(
                transferRequest.getId(),
                transferRequest.getRequestNo(),
                transferRequest.getMemberId(),
                transferRequest.getSourceAccountId(),
                transferRequest.getTargetAccountId(),
                transferRequest.getAmount(),
                bankingBalanceAfter,
                cashBalance,
                reservedCash,
                cashBalance - reservedCash,
                transferRequest.getStatus(),
                transferRequest.getCompletedAt()
        );
    }
}
