package com.pointbank.ledger.message.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.ledger.event.BuyOrderRequestedEvent;
import com.pointbank.ledger.event.LedgerEventType;
import com.pointbank.ledger.event.SellOrderRequestedEvent;
import com.pointbank.ledger.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
@RequiredArgsConstructor
public class LedgerOrderCommandConsumer {
    private static final Logger log = LoggerFactory.getLogger(LedgerOrderCommandConsumer.class);

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;
    private final LedgerOrderCommandHandler ledgerOrderCommandHandler;

    @Scheduled(fixedDelay = 1000)
    public void consume() {
        for (Message message : sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(sqsProperties.ledgerOrderCommandQueueUrl())
                .maxNumberOfMessages(5)
                .waitTimeSeconds(1)
                .build()).messages()) {
            handle(message);
        }
    }

    private void handle(Message message) {
        try {
            JsonNode body = objectMapper.readTree(message.body());
            String eventType = body.path("eventType").asText(null);
            if (LedgerEventType.BUY_ORDER_REQUESTED.equals(eventType)) {
                ledgerOrderCommandHandler.handle(objectMapper.treeToValue(body, BuyOrderRequestedEvent.class));
            } else if (LedgerEventType.SELL_ORDER_REQUESTED.equals(eventType)) {
                ledgerOrderCommandHandler.handle(objectMapper.treeToValue(body, SellOrderRequestedEvent.class));
            } else {
                throw new IllegalArgumentException("Unsupported event type: " + eventType);
            }
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(sqsProperties.ledgerOrderCommandQueueUrl())
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception exception) {
            log.warn("Failed to consume ledger order command message", exception);
        }
    }
}
