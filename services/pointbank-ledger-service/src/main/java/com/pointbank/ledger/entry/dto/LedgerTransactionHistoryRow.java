package com.pointbank.ledger.entry.dto;

import java.time.LocalDateTime;

public class LedgerTransactionHistoryRow {
    private Long transactionId;
    private String transactionType;
    private long amount;
    private long balanceAfter;
    private String description;
    private LocalDateTime createdAt;
    private String transferNo;
    private String fromAccountNumber;
    private String toAccountNumber;

    public Long getTransactionId() { return transactionId; }
    public String getTransactionType() { return transactionType; }
    public long getAmount() { return amount; }
    public long getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getTransferNo() { return transferNo; }
    public String getFromAccountNumber() { return fromAccountNumber; }
    public String getToAccountNumber() { return toAccountNumber; }
}
