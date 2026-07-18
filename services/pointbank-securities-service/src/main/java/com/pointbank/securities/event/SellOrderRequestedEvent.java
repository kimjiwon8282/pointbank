package com.pointbank.securities.event;

import java.time.LocalDateTime;

public record SellOrderRequestedEvent(
        String eventId,
        String eventType,
        String orderNo,
        Long memberId,
        Long securitiesAccountId,
        String stockCode,
        String stockName,
        long quantity,
        long orderPrice,
        long orderAmount,
        long fee,
        long tax,
        long totalAmount,
        LocalDateTime quoteObservedAt
) {
}
