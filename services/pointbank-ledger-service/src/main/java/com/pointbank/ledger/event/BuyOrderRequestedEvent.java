package com.pointbank.ledger.event;

import java.time.LocalDateTime;

public record BuyOrderRequestedEvent(
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
