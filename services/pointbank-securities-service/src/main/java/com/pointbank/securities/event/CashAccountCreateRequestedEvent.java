package com.pointbank.securities.event;

import java.time.LocalDateTime;

public record CashAccountCreateRequestedEvent(
        String eventId,
        String eventType,
        Long memberId,
        Long securitiesAccountId,
        String securitiesAccountNumber,
        LocalDateTime occurredAt
) {
}
