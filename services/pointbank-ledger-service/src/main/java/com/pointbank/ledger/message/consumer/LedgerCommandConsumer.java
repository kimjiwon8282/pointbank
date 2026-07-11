package com.pointbank.ledger.message.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.ledger.event.CashAccountCreateRequestedEvent;
import com.pointbank.ledger.event.LedgerEventType;
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
public class LedgerCommandConsumer {
    private static final Logger log = LoggerFactory.getLogger(LedgerCommandConsumer.class);

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;
    private final LedgerCommandHandler ledgerCommandHandler;

    @Scheduled(fixedDelay = 1000)
    public void consume() {
        for (Message message : sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(sqsProperties.ledgerCommandQueueUrl())
                .maxNumberOfMessages(5)
                .waitTimeSeconds(1)
                .build()).messages()) {
            handle(message);
        }
    }

    private void handle(Message message) {
        try {
            CashAccountCreateRequestedEvent event =
                    objectMapper.readValue(message.body(), CashAccountCreateRequestedEvent.class);
            if (!LedgerEventType.CASH_ACCOUNT_CREATE_REQUESTED.equals(event.eventType())) {
                throw new IllegalArgumentException("Unsupported event type: " + event.eventType());
            }
            ledgerCommandHandler.handle(event);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(sqsProperties.ledgerCommandQueueUrl())
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception exception) {
            log.warn("Failed to consume ledger command message", exception);
        }
    }
}
