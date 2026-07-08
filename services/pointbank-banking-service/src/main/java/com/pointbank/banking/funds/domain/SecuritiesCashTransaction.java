package com.pointbank.banking.funds.domain;

import java.time.LocalDateTime;

public class SecuritiesCashTransaction {

    private Long id;
    private Long securitiesCashAccountId;
    private Long memberId;
    private String requestNo;
    private SecuritiesCashTransactionType transactionType;
    private long amount;
    private long cashBalanceAfter;
    private long reservedCashAfter;
    private String description;
    private LocalDateTime createdAt;

    public SecuritiesCashTransaction() {
    }

    public SecuritiesCashTransaction(
            Long securitiesCashAccountId,
            Long memberId,
            String requestNo,
            SecuritiesCashTransactionType transactionType,
            long amount,
            long cashBalanceAfter,
            long reservedCashAfter,
            String description
    ) {
        this.securitiesCashAccountId = securitiesCashAccountId;
        this.memberId = memberId;
        this.requestNo = requestNo;
        this.transactionType = transactionType;
        this.amount = amount;
        this.cashBalanceAfter = cashBalanceAfter;
        this.reservedCashAfter = reservedCashAfter;
        this.description = description;
    }

    public Long getId() { return id; }
    public Long getSecuritiesCashAccountId() { return securitiesCashAccountId; }
    public Long getMemberId() { return memberId; }
    public String getRequestNo() { return requestNo; }
    public SecuritiesCashTransactionType getTransactionType() { return transactionType; }
    public long getAmount() { return amount; }
    public long getCashBalanceAfter() { return cashBalanceAfter; }
    public long getReservedCashAfter() { return reservedCashAfter; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
