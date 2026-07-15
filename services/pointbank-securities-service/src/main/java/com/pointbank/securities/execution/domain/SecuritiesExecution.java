package com.pointbank.securities.execution.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuritiesExecution {
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long securitiesAccountId;
    private Long memberId;
    private String stockCode;
    private ExecutionSide executionSide;
    private long executionPrice;
    private long quantity;
    private long executionAmount;
    private long fee;
    private long tax;
    private Long buyCost;
    private Long realizedProfit;
    private BigDecimal realizedReturnRate;
    private LocalDateTime executedAt;
}
