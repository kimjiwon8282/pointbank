package com.pointbank.ledger.message.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.ledger.api.dto.SecuritiesBuyDebitRequest;
import com.pointbank.ledger.api.dto.SecuritiesTradeFundsResponse;
import com.pointbank.ledger.api.service.SecuritiesTradeLedgerService;
import com.pointbank.ledger.event.BuyFundsDebitedEvent;
import com.pointbank.ledger.event.BuyFundsFailedEvent;
import com.pointbank.ledger.event.BuyOrderRequestedEvent;
import com.pointbank.ledger.event.LedgerEventType;
import com.pointbank.ledger.event.SellFundsCreditedEvent;
import com.pointbank.ledger.event.SellFundsFailedEvent;
import com.pointbank.ledger.event.SellOrderRequestedEvent;
import com.pointbank.ledger.api.dto.SecuritiesSellCreditRequest;
import com.pointbank.ledger.global.exception.CustomException;
import com.pointbank.ledger.global.exception.ErrorCode;
import com.pointbank.ledger.message.mapper.ProcessedMessageMapper;
import com.pointbank.ledger.outbox.domain.OutboxEvent;
import com.pointbank.ledger.outbox.domain.OutboxEventStatus;
import com.pointbank.ledger.outbox.mapper.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerOrderCommandHandler {

    private final ProcessedMessageMapper processedMessageMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final SecuritiesTradeLedgerService securitiesTradeLedgerService;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    public void handle(BuyOrderRequestedEvent event) {
        try {
            transactionTemplate().executeWithoutResult(status -> debitAndRecordSuccess(event));
        } catch (CustomException exception) {
            if (!isBusinessFailure(exception)) {
                throw exception;
            }
            transactionTemplate().executeWithoutResult(status -> recordFailure(event, exception));
        }
    }

    public void handle(SellOrderRequestedEvent event) {
        try {
            transactionTemplate().executeWithoutResult(status -> creditAndRecordSuccess(event));
        } catch (CustomException exception) {
            if (!isBusinessFailure(exception)) throw exception;
            transactionTemplate().executeWithoutResult(status -> recordFailure(event, exception));
        }
    }

    private void creditAndRecordSuccess(SellOrderRequestedEvent event) {
        if (processedMessageMapper.existsByEventId(event.eventId())) return;
        SecuritiesTradeFundsResponse response = securitiesTradeLedgerService.creditSellFunds(
                event.orderNo(),
                new SecuritiesSellCreditRequest(
                        event.memberId(), event.orderNo(), event.stockCode(), event.orderAmount(),
                        event.fee(), event.tax(), event.totalAmount()));
        outboxEventMapper.insert(createCreditedOutboxEvent(event, response));
        processedMessageMapper.insert(event.eventId(), event.eventType());
    }

    private void recordFailure(SellOrderRequestedEvent event, CustomException exception) {
        if (processedMessageMapper.existsByEventId(event.eventId())) return;
        outboxEventMapper.insert(createFailedOutboxEvent(event, exception.getErrorCode()));
        processedMessageMapper.insert(event.eventId(), event.eventType());
    }

    private OutboxEvent createCreditedOutboxEvent(
            SellOrderRequestedEvent source, SecuritiesTradeFundsResponse response) {
        String eventId = UUID.randomUUID().toString();
        SellFundsCreditedEvent event = new SellFundsCreditedEvent(
                eventId, LedgerEventType.SELL_FUNDS_CREDITED, source.orderNo(), source.memberId(),
                source.securitiesAccountId(), source.stockCode(), source.stockName(), source.quantity(),
                source.orderPrice(), source.orderAmount(), source.fee(), source.tax(), source.totalAmount(),
                source.quoteObservedAt(), response.requestNo(), response.balanceAfter());
        return createOutboxEvent(
                eventId, LedgerEventType.SELL_FUNDS_CREDITED, source.securitiesAccountId(), event);
    }

    private OutboxEvent createFailedOutboxEvent(SellOrderRequestedEvent source, ErrorCode errorCode) {
        String eventId = UUID.randomUUID().toString();
        SellFundsFailedEvent event = new SellFundsFailedEvent(
                eventId, LedgerEventType.SELL_FUNDS_FAILED, source.orderNo(), source.memberId(),
                source.securitiesAccountId(), source.stockCode(), source.stockName(),
                errorCode.getCode(), errorCode.getMessage());
        return createOutboxEvent(
                eventId, LedgerEventType.SELL_FUNDS_FAILED, source.securitiesAccountId(), event);
    }

    private void debitAndRecordSuccess(BuyOrderRequestedEvent event) {
        if (processedMessageMapper.existsByEventId(event.eventId())) {
            return;
        }

        SecuritiesTradeFundsResponse response = securitiesTradeLedgerService.debitBuyFunds(
                event.orderNo(),
                new SecuritiesBuyDebitRequest(
                        event.memberId(),
                        event.orderNo(),
                        event.stockCode(),
                        event.orderAmount(),
                        event.fee(),
                        event.totalAmount()
                )
        );
        OutboxEvent outboxEvent = createDebitedOutboxEvent(event, response);
        processedMessageMapper.insert(event.eventId(), event.eventType());
        outboxEventMapper.insert(outboxEvent);
    }

    private void recordFailure(BuyOrderRequestedEvent event, CustomException exception) {
        if (processedMessageMapper.existsByEventId(event.eventId())) {
            return;
        }

        OutboxEvent outboxEvent = createFailedOutboxEvent(event, exception.getErrorCode());
        processedMessageMapper.insert(event.eventId(), event.eventType());
        outboxEventMapper.insert(outboxEvent);
    }

    private OutboxEvent createDebitedOutboxEvent(
            BuyOrderRequestedEvent sourceEvent,
            SecuritiesTradeFundsResponse response
    ) {
        String eventId = UUID.randomUUID().toString();
        BuyFundsDebitedEvent event = new BuyFundsDebitedEvent(
                eventId,
                LedgerEventType.BUY_FUNDS_DEBITED,
                sourceEvent.orderNo(),
                sourceEvent.memberId(),
                sourceEvent.securitiesAccountId(),
                sourceEvent.stockCode(),
                sourceEvent.stockName(),
                sourceEvent.quantity(),
                sourceEvent.orderPrice(),
                sourceEvent.orderAmount(),
                sourceEvent.fee(),
                sourceEvent.tax(),
                sourceEvent.totalAmount(),
                sourceEvent.quoteObservedAt(),
                response.requestNo(),
                response.balanceAfter()
        );
        return createOutboxEvent(
                eventId,
                LedgerEventType.BUY_FUNDS_DEBITED,
                sourceEvent.securitiesAccountId(),
                event
        );
    }

    private OutboxEvent createFailedOutboxEvent(BuyOrderRequestedEvent sourceEvent, ErrorCode errorCode) {
        String eventId = UUID.randomUUID().toString();
        BuyFundsFailedEvent event = new BuyFundsFailedEvent(
                eventId,
                LedgerEventType.BUY_FUNDS_FAILED,
                sourceEvent.orderNo(),
                sourceEvent.memberId(),
                sourceEvent.securitiesAccountId(),
                sourceEvent.stockCode(),
                sourceEvent.stockName(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
        return createOutboxEvent(
                eventId,
                LedgerEventType.BUY_FUNDS_FAILED,
                sourceEvent.securitiesAccountId(),
                event
        );
    }

    private OutboxEvent createOutboxEvent(String eventId, String eventType, Long aggregateId, Object payload) {
        try {
            return new OutboxEvent(
                    eventId,
                    eventType,
                    "SECURITIES_ORDER",
                    aggregateId,
                    objectMapper.writeValueAsString(payload),
                    OutboxEventStatus.PENDING
            );
        } catch (JsonProcessingException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isBusinessFailure(CustomException exception) {
        return exception.getErrorCode() != ErrorCode.INTERNAL_SERVER_ERROR;
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }
}
