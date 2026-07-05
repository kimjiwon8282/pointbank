package com.pointbank.banking.transaction.dto;

import com.pointbank.banking.transaction.domain.AccountTransactionType;
import com.pointbank.banking.transaction.domain.TransactionDirection;

import java.time.LocalDateTime;

public record TransactionHistoryItemResponse(
        Long transactionId,
        AccountTransactionType transactionType,
        TransactionDirection direction,
        String title,
        long amount,
        long signedAmount,
        long balanceAfter,
        String counterpartyAccountNumber,
        String transferNo,
        LocalDateTime createdAt
) {
    public static TransactionHistoryItemResponse from(TransactionHistoryRow row) {
        TransactionDirection direction = TransactionDirection.from(row.getTransactionType());
        String title = row.getDescription() != null && !row.getDescription().isBlank()
                ? row.getDescription() : defaultTitle(row.getTransactionType());
        String counterparty = switch (row.getTransactionType()) {
            case DEPOSIT -> null;
            case TRANSFER_OUT -> row.getToAccountNumber();
            case TRANSFER_IN -> row.getFromAccountNumber();
        };
        long signedAmount = direction == TransactionDirection.IN
                ? row.getAmount() : -row.getAmount();
        return new TransactionHistoryItemResponse(
                row.getTransactionId(), row.getTransactionType(), direction, title,
                row.getAmount(), signedAmount, row.getBalanceAfter(), counterparty,
                row.getTransferNo(), row.getCreatedAt());
    }

    private static String defaultTitle(AccountTransactionType type) {
        return switch (type) {
            case DEPOSIT -> "개발용 포인트 충전";
            case TRANSFER_OUT -> "포인트 송금 출금";
            case TRANSFER_IN -> "포인트 송금 입금";
        };
    }
}
