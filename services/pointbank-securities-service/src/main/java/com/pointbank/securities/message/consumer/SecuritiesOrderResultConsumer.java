package com.pointbank.securities.message.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.securities.event.BuyFundsDebitedEvent;
import com.pointbank.securities.event.BuyFundsFailedEvent;
import com.pointbank.securities.event.SecuritiesEventType;
import com.pointbank.securities.event.SellFundsCreditedEvent;
import com.pointbank.securities.event.SellFundsFailedEvent;
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
public class SecuritiesOrderResultConsumer {
    private static final Logger log = LoggerFactory.getLogger(SecuritiesOrderResultConsumer.class);

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;
    private final SecuritiesOrderResultHandler securitiesOrderResultHandler;

    @Scheduled(fixedDelay = 1000)
    public void consume() {
        for (Message message : sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(sqsProperties.securitiesOrderResultQueueUrl())
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
                BuyFundsDebitedEvent event = objectMapper.treeToValue(body, BuyFundsDebitedEvent.class);
                securitiesOrderResultHandler.handle(event);
            } else if (SecuritiesEventType.BUY_FUNDS_FAILED.equals(eventType)) {
                BuyFundsFailedEvent event = objectMapper.treeToValue(body, BuyFundsFailedEvent.class);
                securitiesOrderResultHandler.handle(event);
            } else if (SecuritiesEventType.SELL_FUNDS_CREDITED.equals(eventType)) {
                securitiesOrderResultHandler.handle(
                        objectMapper.treeToValue(body, SellFundsCreditedEvent.class));
            } else if (SecuritiesEventType.SELL_FUNDS_FAILED.equals(eventType)) {
                securitiesOrderResultHandler.handle(
                        objectMapper.treeToValue(body, SellFundsFailedEvent.class));
            } else {
                throw new IllegalArgumentException("Unsupported securities order result event type: " + eventType);
            }
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(sqsProperties.securitiesOrderResultQueueUrl())
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception exception) {
            log.warn("Failed to consume securities order result message", exception);
        }
    }
}
