package com.pointbank.banking.funds.domain;

import java.time.LocalDateTime;

public class FundTransferRequest {

    private Long id;
    private String requestNo;
    private Long memberId;
    private FundAccountType sourceType;
    private FundAccountType targetType;
    private Long sourceAccountId;
    private Long targetAccountId;
    private long amount;
    private Long bankingBalanceAfter;
    private FundTransferStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    public FundTransferRequest() {
    }

    public FundTransferRequest(
            String requestNo,
            Long memberId,
            FundAccountType sourceType,
            FundAccountType targetType,
            long amount,
            FundTransferStatus status
    ) {
        this.requestNo = requestNo;
        this.memberId = memberId;
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.amount = amount;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getRequestNo() { return requestNo; }
    public Long getMemberId() { return memberId; }
    public FundAccountType getSourceType() { return sourceType; }
    public FundAccountType getTargetType() { return targetType; }
    public Long getSourceAccountId() { return sourceAccountId; }
    public Long getTargetAccountId() { return targetAccountId; }
    public long getAmount() { return amount; }
    public Long getBankingBalanceAfter() { return bankingBalanceAfter; }
    public FundTransferStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
