package com.pointbank.ledger.account.domain;

import java.time.LocalDateTime;

public class LedgerAccount {
    private Long id;
    private Long memberId;
    private LedgerAccountType accountType;
    private String accountNumber;
    private String accountPasswordHash;
    private long balance;
    private long reservedBalance;
    private LedgerAccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LedgerAccount() {
    }

    public LedgerAccount(
            Long memberId,
            LedgerAccountType accountType,
            String accountNumber,
            String accountPasswordHash,
            long balance,
            long reservedBalance,
            LedgerAccountStatus status
    ) {
        this.memberId = memberId;
        this.accountType = accountType;
        this.accountNumber = accountNumber;
        this.accountPasswordHash = accountPasswordHash;
        this.balance = balance;
        this.reservedBalance = reservedBalance;
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public LedgerAccountType getAccountType() { return accountType; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountPasswordHash() { return accountPasswordHash; }
    public long getBalance() { return balance; }
    public long getReservedBalance() { return reservedBalance; }
    public LedgerAccountStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
