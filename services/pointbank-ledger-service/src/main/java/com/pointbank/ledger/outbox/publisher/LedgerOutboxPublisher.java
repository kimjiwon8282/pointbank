package com.pointbank.ledger.outbox.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.ledger.event.BuyFundsDebitedEvent;
import com.pointbank.ledger.event.BuyFundsFailedEvent;
import com.pointbank.ledger.event.CashAccountCreatedEvent;
import com.pointbank.ledger.event.LedgerEventType;
import com.pointbank.ledger.event.SellFundsCreditedEvent;
import com.pointbank.ledger.event.SellFundsFailedEvent;
import com.pointbank.ledger.outbox.domain.OutboxEvent;
import com.pointbank.ledger.outbox.mapper.OutboxEventMapper;
import com.pointbank.ledger.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
@RequiredArgsConstructor
public class LedgerOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxPublisher.class);
    private static final int BATCH_SIZE = 10;
    private static final int MAX_RETRY_COUNT = 5;

    private final OutboxEventMapper outboxEventMapper;
    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 2000)
    public void publishPendingEvents() {
        publishPendingEvents(LedgerEventType.CASH_ACCOUNT_CREATED);
        publishPendingEvents(LedgerEventType.BUY_FUNDS_DEBITED);
        publishPendingEvents(LedgerEventType.BUY_FUNDS_FAILED);
        publishPendingEvents(LedgerEventType.SELL_FUNDS_CREDITED);
        publishPendingEvents(LedgerEventType.SELL_FUNDS_FAILED);
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
            log.warn("Failed to publish ledger outbox event {}", outboxEvent.getEventId(), exception);
            if (outboxEvent.getRetryCount() + 1 >= MAX_RETRY_COUNT) {
                outboxEventMapper.markFailed(outboxEvent.getEventId());
                return;
            }
            outboxEventMapper.incrementRetryCount(outboxEvent.getEventId());
        }
    }

    private PublishDestination resolveDestination(OutboxEvent outboxEvent) throws Exception {
        if (LedgerEventType.CASH_ACCOUNT_CREATED.equals(outboxEvent.getEventType())) {
            CashAccountCreatedEvent event =
                    objectMapper.readValue(outboxEvent.getPayload(), CashAccountCreatedEvent.class);
            return new PublishDestination(
                    sqsProperties.securitiesResultQueueUrl(),
                    String.valueOf(event.securitiesAccountId())
            );
        }
        if (LedgerEventType.BUY_FUNDS_DEBITED.equals(outboxEvent.getEventType())) {
            BuyFundsDebitedEvent event =
                    objectMapper.readValue(outboxEvent.getPayload(), BuyFundsDebitedEvent.class);
            return new PublishDestination(
                    sqsProperties.securitiesOrderResultQueueUrl(),
                    String.valueOf(event.memberId())
            );
        }
        if (LedgerEventType.BUY_FUNDS_FAILED.equals(outboxEvent.getEventType())) {
            BuyFundsFailedEvent event =
                    objectMapper.readValue(outboxEvent.getPayload(), BuyFundsFailedEvent.class);
            return new PublishDestination(
                    sqsProperties.securitiesOrderResultQueueUrl(),
                    String.valueOf(event.memberId())
            );
        }
        if (LedgerEventType.SELL_FUNDS_CREDITED.equals(outboxEvent.getEventType())) {
            SellFundsCreditedEvent event =
                    objectMapper.readValue(outboxEvent.getPayload(), SellFundsCreditedEvent.class);
            return new PublishDestination(
                    sqsProperties.securitiesOrderResultQueueUrl(), String.valueOf(event.memberId()));
        }
        if (LedgerEventType.SELL_FUNDS_FAILED.equals(outboxEvent.getEventType())) {
            SellFundsFailedEvent event =
                    objectMapper.readValue(outboxEvent.getPayload(), SellFundsFailedEvent.class);
            return new PublishDestination(
                    sqsProperties.securitiesOrderResultQueueUrl(), String.valueOf(event.memberId()));
        }
        throw new IllegalArgumentException("Unsupported ledger outbox event type: " + outboxEvent.getEventType());
    }

    private record PublishDestination(String queueUrl, String messageGroupId) {
    }
}
