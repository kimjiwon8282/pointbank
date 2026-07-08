package com.pointbank.securities.account.domain;

import java.time.LocalDateTime;

public class SecuritiesAccount {

    private Long id;
    private Long memberId;
    private String accountNumber;
    private String accountPasswordHash;
    private SecuritiesAccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SecuritiesAccount() {
    }

    public SecuritiesAccount(
            Long memberId,
            String accountNumber,
            String accountPasswordHash,
            SecuritiesAccountStatus status
    ) {
        this.memberId = memberId;
        this.accountNumber = accountNumber;
        this.accountPasswordHash = accountPasswordHash;
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountPasswordHash() { return accountPasswordHash; }
    public SecuritiesAccountStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
