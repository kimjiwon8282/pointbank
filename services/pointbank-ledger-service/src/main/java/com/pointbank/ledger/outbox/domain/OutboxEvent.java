package com.pointbank.ledger.outbox.domain;

import java.time.LocalDateTime;

public class OutboxEvent {
    private Long id;
    private String eventId;
    private String eventType;
    private String aggregateType;
    private Long aggregateId;
    private String payload;
    private OutboxEventStatus status;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;

    public OutboxEvent() {
    }

    public OutboxEvent(
            String eventId,
            String eventType,
            String aggregateType,
            Long aggregateId,
            String payload,
            OutboxEventStatus status
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getAggregateType() { return aggregateType; }
    public Long getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
    public OutboxEventStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
