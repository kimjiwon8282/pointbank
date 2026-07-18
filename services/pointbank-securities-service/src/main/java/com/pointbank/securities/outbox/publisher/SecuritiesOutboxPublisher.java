package com.pointbank.securities.outbox.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.securities.event.BuyOrderRequestedEvent;
import com.pointbank.securities.event.CashAccountCreateRequestedEvent;
import com.pointbank.securities.event.SellOrderRequestedEvent;
import com.pointbank.securities.event.SecuritiesEventType;
import com.pointbank.securities.outbox.domain.OutboxEvent;
import com.pointbank.securities.outbox.mapper.OutboxEventMapper;
import com.pointbank.securities.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
@RequiredArgsConstructor
public class SecuritiesOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(SecuritiesOutboxPublisher.class);
    private static final int BATCH_SIZE = 10;
    private static final int MAX_RETRY_COUNT = 5;

    private final OutboxEventMapper outboxEventMapper;
    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 2000)
    public void publishPendingEvents() {
        publishPendingEvents(SecuritiesEventType.CASH_ACCOUNT_CREATE_REQUESTED);
        publishPendingEvents(SecuritiesEventType.BUY_ORDER_REQUESTED);
        publishPendingEvents(SecuritiesEventType.SELL_ORDER_REQUESTED);
    }

    private void publishPendingEvents(String eventType) {
        outboxEventMapper.findPendingByEventType(eventType, BATCH_SIZE).forEach(this::publish);
    }

    private void publish(OutboxEvent outboxEvent) {
        try {
            PublishDestination destination = resolveDestination(outboxEvent);
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(destination.queueUrl())
                    .messageBody(outboxEvent.getPayload())
                    .messageGroupId(destination.messageGroupId())
                    .messageDeduplicationId(outboxEvent.getEventId())
                    .build());
            outboxEventMapper.markPublished(outboxEvent.getEventId());
        } catch (Exception exception) {
            log.warn("Failed to publish securities outbox event {}", outboxEvent.getEventId(), exception);
            if (outboxEvent.getRetryCount() + 1 >= MAX_RETRY_COUNT) {
                outboxEventMapper.markFailed(outboxEvent.getEventId());
                return;
            }
            outboxEventMapper.incrementRetryCount(outboxEvent.getEventId());
        }
    }

    private PublishDestination resolveDestination(OutboxEvent outboxEvent) throws Exception {
        if (SecuritiesEventType.CASH_ACCOUNT_CREATE_REQUESTED.equals(outboxEvent.getEventType())) {
            CashAccountCreateRequestedEvent event =
                    objectMapper.readValue(outboxEvent.getPayload(), CashAccountCreateRequestedEvent.class);
            return new PublishDestination(
                    sqsProperties.ledgerCommandQueueUrl(),
                    String.valueOf(event.securitiesAccountId())
            );
        }
        if (SecuritiesEventType.BUY_ORDER_REQUESTED.equals(outboxEvent.getEventType())) {
            BuyOrderRequestedEvent event =
                    objectMapper.readValue(outboxEvent.getPayload(), BuyOrderRequestedEvent.class);
            return new PublishDestination(
                    sqsProperties.ledgerOrderCommandQueueUrl(),
                    String.valueOf(event.memberId())
            );
        }
        if (SecuritiesEventType.SELL_ORDER_REQUESTED.equals(outboxEvent.getEventType())) {
            SellOrderRequestedEvent event =
                    objectMapper.readValue(outboxEvent.getPayload(), SellOrderRequestedEvent.class);
            return new PublishDestination(
                    sqsProperties.ledgerOrderCommandQueueUrl(),
                    String.valueOf(event.memberId())
            );
        }
        throw new IllegalArgumentException("Unsupported securities outbox event type: " + outboxEvent.getEventType());
    }

    private record PublishDestination(String queueUrl, String messageGroupId) {
    }
}
