package com.pointbank.banking.transaction.domain;

public enum TransactionDirection {
    IN,
    OUT;

    public static TransactionDirection from(AccountTransactionType type) {
        return switch (type) {
            case DEPOSIT, TRANSFER_IN -> IN;
            case TRANSFER_OUT, SECURITIES_DEPOSIT_OUT -> OUT;
        };
    }
}
