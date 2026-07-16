package com.pointbank.ledger.message.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.ledger.event.BuyOrderRequestedEvent;
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
public class LedgerOrderCommandDlqConsumer {
    private static final Logger log = LoggerFactory.getLogger(LedgerOrderCommandDlqConsumer.class);

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;
    private final LedgerOrderCommandDlqHandler dlqHandler;

    @Scheduled(fixedDelay = 3000)
    public void consume() {
        for (Message message : sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(sqsProperties.ledgerOrderCommandDlqUrl())
                .maxNumberOfMessages(5)
                .waitTimeSeconds(1)
                .build()).messages()) {
            handle(message);
        }
    }

    private void handle(Message message) {
        try {
            BuyOrderRequestedEvent event =
                    objectMapper.readValue(message.body(), BuyOrderRequestedEvent.class);
            if (!LedgerEventType.BUY_ORDER_REQUESTED.equals(event.eventType())) {
                throw new IllegalArgumentException("Unsupported ledger order command DLQ event type.");
            }
            dlqHandler.handle(event);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(sqsProperties.ledgerOrderCommandDlqUrl())
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception exception) {
            log.warn("Failed to apply ledger order command DLQ withdrawal policy", exception);
        }
    }
}
