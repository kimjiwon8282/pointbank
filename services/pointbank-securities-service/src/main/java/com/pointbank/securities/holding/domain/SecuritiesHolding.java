package com.pointbank.securities.holding.domain;

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
public class SecuritiesHolding {
    private Long id;
    private Long securitiesAccountId;
    private Long memberId;
    private String stockCode;
    private long quantity;
    private long reservedQuantity;
    private BigDecimal avgBuyPrice;
    private long totalBuyAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
