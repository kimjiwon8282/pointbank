package com.pointbank.banking.transaction.domain;

import java.util.List;

public enum TransactionQueryType {
    ALL,
    IN,
    OUT;

    public List<AccountTransactionType> toTransactionTypes() {
        return switch (this) {
            case ALL -> List.of(AccountTransactionType.DEPOSIT,
                    AccountTransactionType.TRANSFER_IN, AccountTransactionType.TRANSFER_OUT);
            case IN -> List.of(AccountTransactionType.DEPOSIT, AccountTransactionType.TRANSFER_IN);
            case OUT -> List.of(AccountTransactionType.TRANSFER_OUT);
        };
    }
}
