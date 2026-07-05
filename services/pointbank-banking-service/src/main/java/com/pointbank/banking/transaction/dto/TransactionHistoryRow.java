package com.pointbank.banking.transaction.dto;

import com.pointbank.banking.transaction.domain.AccountTransactionType;

import java.time.LocalDateTime;

public class TransactionHistoryRow {
    private Long transactionId;
    private AccountTransactionType transactionType;
    private long amount;
    private long balanceAfter;
    private String description;
    private LocalDateTime createdAt;
    private String transferNo;
    private String fromAccountNumber;
    private String toAccountNumber;

    public TransactionHistoryRow() {
    }

    public Long getTransactionId() { return transactionId; }
    public AccountTransactionType getTransactionType() { return transactionType; }
    public long getAmount() { return amount; }
    public long getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getTransferNo() { return transferNo; }
    public String getFromAccountNumber() { return fromAccountNumber; }
    public String getToAccountNumber() { return toAccountNumber; }
}
