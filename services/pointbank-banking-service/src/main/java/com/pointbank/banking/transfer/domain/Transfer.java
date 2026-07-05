package com.pointbank.banking.transfer.domain;

import java.time.LocalDateTime;

public class Transfer {
    private Long id;
    private String transferNo;
    private Long fromAccountId;
    private Long toAccountId;
    private Long fromMemberId;
    private Long toMemberId;
    private long amount;
    private TransferStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public Transfer() {
    }

    public Transfer(String transferNo, Long fromAccountId, Long toAccountId,
                    Long fromMemberId, Long toMemberId, long amount,
                    TransferStatus status, String failureReason, LocalDateTime completedAt) {
        this.transferNo = transferNo;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.fromMemberId = fromMemberId;
        this.toMemberId = toMemberId;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
        this.completedAt = completedAt;
    }

    public static Transfer requested(String transferNo, Long fromAccountId, Long toAccountId,
                                     Long fromMemberId, Long toMemberId, long amount) {
        return new Transfer(transferNo, fromAccountId, toAccountId, fromMemberId, toMemberId,
                amount, TransferStatus.REQUESTED, null, null);
    }

    public static Transfer completed(String transferNo, Long fromAccountId, Long toAccountId,
                                     Long fromMemberId, Long toMemberId, long amount,
                                     LocalDateTime completedAt) {
        return new Transfer(transferNo, fromAccountId, toAccountId, fromMemberId, toMemberId,
                amount, TransferStatus.COMPLETED, null, completedAt);
    }

    public Long getId() { return id; }
    public String getTransferNo() { return transferNo; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public Long getFromMemberId() { return fromMemberId; }
    public Long getToMemberId() { return toMemberId; }
    public long getAmount() { return amount; }
    public TransferStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
