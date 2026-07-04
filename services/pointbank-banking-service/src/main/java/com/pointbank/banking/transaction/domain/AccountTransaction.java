package com.pointbank.banking.transaction.domain;

import java.time.LocalDateTime;

public class AccountTransaction {

    private Long id;
    private Long accountId;
    private Long memberId;
    private AccountTransactionType transactionType;
    private long amount;
    private long balanceAfter;
    private String description;
    private LocalDateTime createdAt;

    public AccountTransaction() {
    }

    public AccountTransaction(
            Long accountId,
            Long memberId,
            AccountTransactionType transactionType,
            long amount,
            long balanceAfter,
            String description
    ) {
        this.accountId = accountId;
        this.memberId = memberId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public Long getMemberId() { return memberId; }
    public AccountTransactionType getTransactionType() { return transactionType; }
    public long getAmount() { return amount; }
    public long getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
