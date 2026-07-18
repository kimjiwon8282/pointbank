package com.pointbank.securities.order.dto;

import java.time.LocalDateTime;

public record SellOrderResponse(
        String orderNo,
        String status,
        String stockCode,
        String stockName,
        long quantity,
        long orderPrice,
        long orderAmount,
        long fee,
        long tax,
        long netAmount,
        LocalDateTime quoteObservedAt,
        String message
) {
}
