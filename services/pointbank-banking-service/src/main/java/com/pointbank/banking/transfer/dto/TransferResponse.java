package com.pointbank.banking.transfer.dto;

import com.pointbank.banking.transfer.domain.TransferStatus;
import java.time.LocalDateTime;

public record TransferResponse(
        String transferNo,
        String fromAccountNumber,
        String toAccountNumber,
        long amount,
        long fromBalanceAfter,
        TransferStatus status,
        LocalDateTime completedAt
) {
}
