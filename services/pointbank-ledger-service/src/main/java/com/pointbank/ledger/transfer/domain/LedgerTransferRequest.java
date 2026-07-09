package com.pointbank.ledger.transfer.domain;

import java.time.LocalDateTime;

public class LedgerTransferRequest {
    private Long id;
    private String requestNo;
    private LedgerTransferType transferType;
    private Long sourceAccountId;
    private Long targetAccountId;
    private Long fromMemberId;
    private Long toMemberId;
    private long amount;
    private Long sourceBalanceAfter;
    private Long targetBalanceAfter;
    private LedgerTransferStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    public LedgerTransferRequest() {
    }

    public LedgerTransferRequest(
            String requestNo,
            LedgerTransferType transferType,
            Long sourceAccountId,
            Long targetAccountId,
            Long fromMemberId,
            Long toMemberId,
            long amount,
            LedgerTransferStatus status
    ) {
        this.requestNo = requestNo;
        this.transferType = transferType;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.fromMemberId = fromMemberId;
        this.toMemberId = toMemberId;
        this.amount = amount;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getRequestNo() { return requestNo; }
    public LedgerTransferType getTransferType() { return transferType; }
    public Long getSourceAccountId() { return sourceAccountId; }
    public Long getTargetAccountId() { return targetAccountId; }
    public Long getFromMemberId() { return fromMemberId; }
    public Long getToMemberId() { return toMemberId; }
    public long getAmount() { return amount; }
    public Long getSourceBalanceAfter() { return sourceBalanceAfter; }
    public Long getTargetBalanceAfter() { return targetBalanceAfter; }
    public LedgerTransferStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
