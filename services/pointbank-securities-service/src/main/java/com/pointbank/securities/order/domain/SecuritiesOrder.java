package com.pointbank.securities.order.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuritiesOrder {
    private Long id;
    private String orderNo;
    private String idempotencyKey;
    private Long securitiesAccountId;
    private Long memberId;
    private String stockCode;
    private OrderSide orderSide;
    private long quantity;
    private long orderPrice;
    private long orderAmount;
    private long fee;
    private long tax;
    private long totalAmount;
    private LocalDateTime quoteObservedAt;
    private OrderStatus status;
    private String failureReason;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
