package com.pointbank.securities.message.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.securities.event.CashAccountCreatedEvent;
import com.pointbank.securities.event.SecuritiesEventType;
import com.pointbank.securities.sqs.SqsProperties;
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
public class SecuritiesResultConsumer {
    private static final Logger log = LoggerFactory.getLogger(SecuritiesResultConsumer.class);

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;
    private final SecuritiesResultHandler securitiesResultHandler;

    @Scheduled(fixedDelay = 1000)
    public void consume() {
        for (Message message : sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(sqsProperties.securitiesResultQueueUrl())
                .maxNumberOfMessages(5)
                .waitTimeSeconds(1)
                .build()).messages()) {
            handle(message);
        }
    }

    private void handle(Message message) {
        try {
            CashAccountCreatedEvent event = objectMapper.readValue(message.body(), CashAccountCreatedEvent.class);
            if (!SecuritiesEventType.CASH_ACCOUNT_CREATED.equals(event.eventType())) {
                throw new IllegalArgumentException("Unsupported event type: " + event.eventType());
            }
            securitiesResultHandler.handle(event);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(sqsProperties.securitiesResultQueueUrl())
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception exception) {
            log.warn("Failed to consume securities result message", exception);
        }
    }
}
