package com.pointbank.securities.order.dto;

import java.time.LocalDateTime;

public record BuyOrderResponse(
        String orderNo,
        String status,
        String stockCode,
        String stockName,
        long quantity,
        long orderPrice,
        long orderAmount,
        long fee,
        long totalAmount,
        LocalDateTime quoteObservedAt,
        String message
) {
}
