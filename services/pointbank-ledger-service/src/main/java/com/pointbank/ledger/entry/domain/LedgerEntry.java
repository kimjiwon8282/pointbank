package com.pointbank.ledger.entry.domain;

import com.pointbank.ledger.account.domain.LedgerAccountType;

import java.time.LocalDateTime;

public class LedgerEntry {
    private Long id;
    private String requestNo;
    private Long transferRequestId;
    private Long ledgerAccountId;
    private Long memberId;
    private LedgerAccountType accountType;
    private LedgerEntryType entryType;
    private LedgerEntryDirection direction;
    private long amount;
    private long balanceAfter;
    private long reservedBalanceAfter;
    private String description;
    private LocalDateTime createdAt;

    public LedgerEntry() {
    }

    public LedgerEntry(
            String requestNo,
            Long transferRequestId,
            Long ledgerAccountId,
            Long memberId,
            LedgerAccountType accountType,
            LedgerEntryType entryType,
            LedgerEntryDirection direction,
            long amount,
            long balanceAfter,
            long reservedBalanceAfter,
            String description
    ) {
        this.requestNo = requestNo;
        this.transferRequestId = transferRequestId;
        this.ledgerAccountId = ledgerAccountId;
        this.memberId = memberId;
        this.accountType = accountType;
        this.entryType = entryType;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.reservedBalanceAfter = reservedBalanceAfter;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getRequestNo() { return requestNo; }
    public Long getTransferRequestId() { return transferRequestId; }
    public Long getLedgerAccountId() { return ledgerAccountId; }
    public Long getMemberId() { return memberId; }
    public LedgerAccountType getAccountType() { return accountType; }
    public LedgerEntryType getEntryType() { return entryType; }
    public LedgerEntryDirection getDirection() { return direction; }
    public long getAmount() { return amount; }
    public long getBalanceAfter() { return balanceAfter; }
    public long getReservedBalanceAfter() { return reservedBalanceAfter; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
