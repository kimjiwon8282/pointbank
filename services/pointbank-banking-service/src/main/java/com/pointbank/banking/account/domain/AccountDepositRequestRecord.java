package com.pointbank.banking.account.domain;

import java.time.LocalDateTime;

public class AccountDepositRequestRecord {

    private Long id;
    private String requestNo;
    private Long memberId;
    private Long accountId;
    private long amount;
    private Long balanceAfter;
    private AccountDepositStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    public AccountDepositRequestRecord() {
    }

    public AccountDepositRequestRecord(
            String requestNo,
            Long memberId,
            long amount,
            AccountDepositStatus status
    ) {
        this.requestNo = requestNo;
        this.memberId = memberId;
        this.amount = amount;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getRequestNo() { return requestNo; }
    public Long getMemberId() { return memberId; }
    public Long getAccountId() { return accountId; }
    public long getAmount() { return amount; }
    public Long getBalanceAfter() { return balanceAfter; }
    public AccountDepositStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
