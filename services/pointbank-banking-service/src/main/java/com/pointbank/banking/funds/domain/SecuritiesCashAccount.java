package com.pointbank.banking.funds.domain;

import java.time.LocalDateTime;

public class SecuritiesCashAccount {

    private Long id;
    private Long memberId;
    private long cashBalance;
    private long reservedCash;
    private SecuritiesCashAccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SecuritiesCashAccount() {
    }

    public SecuritiesCashAccount(
            Long memberId,
            long cashBalance,
            long reservedCash,
            SecuritiesCashAccountStatus status
    ) {
        this.memberId = memberId;
        this.cashBalance = cashBalance;
        this.reservedCash = reservedCash;
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public long getCashBalance() { return cashBalance; }
    public long getReservedCash() { return reservedCash; }
    public SecuritiesCashAccountStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
