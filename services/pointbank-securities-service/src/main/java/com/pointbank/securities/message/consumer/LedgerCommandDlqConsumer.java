package com.pointbank.securities.message.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.securities.account.service.PendingSecuritiesAccountCleanupService;
import com.pointbank.securities.event.CashAccountCreateRequestedEvent;
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
public class LedgerCommandDlqConsumer {
    private static final Logger log = LoggerFactory.getLogger(LedgerCommandDlqConsumer.class);

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;
    private final PendingSecuritiesAccountCleanupService cleanupService;

    @Scheduled(fixedDelay = 3000)
    public void consume() {
        for (Message message : sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(sqsProperties.ledgerCommandDlqUrl())
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
            if (!SecuritiesEventType.CASH_ACCOUNT_CREATE_REQUESTED.equals(event.eventType())) {
                throw new IllegalArgumentException("Unsupported event type: " + event.eventType());
            }
            cleanupService.cleanupCreateRequestedDlq(event);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(sqsProperties.ledgerCommandDlqUrl())
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception exception) {
            log.warn("Failed to cleanup ledger command DLQ message", exception);
        }
    }
}
