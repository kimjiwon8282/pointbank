package com.pointbank.ledger.event;

import java.time.LocalDateTime;

public record CashAccountCreatedEvent(
        String eventId,
        String eventType,
        String sourceEventId,
        Long memberId,
        Long securitiesAccountId,
        Long ledgerAccountId,
        LocalDateTime occurredAt
) {
}
