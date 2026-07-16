package com.pointbank.securities.order.dto;

import java.time.LocalDateTime;

public record OrderDetailResponse(
        String orderNo,
        String status,
        String stockCode,
        String stockName,
        long quantity,
        long orderPrice,
        long orderAmount,
        long fee,
        long tax,
        long totalAmount,
        LocalDateTime quoteObservedAt,
        String failureReason,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        Long executionPrice,
        LocalDateTime executedAt
) {
}
