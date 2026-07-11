package com.pointbank.securities.outbox.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.securities.event.CashAccountCreateRequestedEvent;
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
        for (OutboxEvent event : outboxEventMapper.findPendingByEventType(
                SecuritiesEventType.CASH_ACCOUNT_CREATE_REQUESTED,
                BATCH_SIZE
        )) {
            publish(event);
        }
    }

    private void publish(OutboxEvent outboxEvent) {
        try {
            CashAccountCreateRequestedEvent event =
                    objectMapper.readValue(outboxEvent.getPayload(), CashAccountCreateRequestedEvent.class);
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(sqsProperties.ledgerCommandQueueUrl())
                    .messageBody(outboxEvent.getPayload())
                    .messageGroupId(String.valueOf(event.securitiesAccountId()))
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
}
