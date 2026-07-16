package com.pointbank.securities.event;

public record BuyFundsFailedEvent(
        String eventId,
        String eventType,
        String orderNo,
        Long memberId,
        Long securitiesAccountId,
        String stockCode,
        String stockName,
        String reasonCode,
        String reasonMessage
) {
}
