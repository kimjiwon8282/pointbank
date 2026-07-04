package com.pointbank.banking.account.domain;

import java.time.LocalDateTime;

public class Account {

    private Long id;
    private Long memberId;
    private String accountNumber;
    private String accountPasswordHash;
    private long balance;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Account() {
    }

    public Account(
            Long memberId,
            String accountNumber,
            String accountPasswordHash,
            long balance,
            AccountStatus status
    ) {
        this.memberId = memberId;
        this.accountNumber = accountNumber;
        this.accountPasswordHash = accountPasswordHash;
        this.balance = balance;
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountPasswordHash() { return accountPasswordHash; }
    public long getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
