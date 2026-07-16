package com.pointbank.securities.message.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.securities.event.BuyFundsDebitedEvent;
import com.pointbank.securities.event.BuyFundsFailedEvent;
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
public class SecuritiesOrderResultDlqConsumer {
    private static final Logger log = LoggerFactory.getLogger(SecuritiesOrderResultDlqConsumer.class);

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;
    private final SecuritiesOrderResultDlqHandler dlqHandler;

    @Scheduled(fixedDelay = 3000)
    public void consume() {
        for (Message message : sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(sqsProperties.securitiesOrderResultDlqUrl())
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
            if (SecuritiesEventType.BUY_FUNDS_DEBITED.equals(eventType)) {
                dlqHandler.handle(objectMapper.treeToValue(body, BuyFundsDebitedEvent.class));
            } else if (SecuritiesEventType.BUY_FUNDS_FAILED.equals(eventType)) {
                dlqHandler.handle(objectMapper.treeToValue(body, BuyFundsFailedEvent.class));
            } else {
                throw new IllegalArgumentException("Unsupported order result DLQ event type: " + eventType);
            }
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(sqsProperties.securitiesOrderResultDlqUrl())
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception exception) {
            log.warn("Failed to apply order result DLQ withdrawal policy", exception);
        }
    }
}
