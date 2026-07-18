package com.pointbank.ledger.message.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.ledger.event.BuyFundsDebitedEvent;
import com.pointbank.ledger.event.BuyFundsFailedEvent;
import com.pointbank.ledger.event.BuyOrderRequestedEvent;
import com.pointbank.ledger.event.LedgerEventType;
import com.pointbank.ledger.event.SellFundsCreditedEvent;
import com.pointbank.ledger.event.SellFundsFailedEvent;
import com.pointbank.ledger.event.SellOrderRequestedEvent;
import com.pointbank.ledger.global.exception.CustomException;
import com.pointbank.ledger.global.exception.ErrorCode;
import com.pointbank.ledger.outbox.domain.OutboxEvent;
import com.pointbank.ledger.outbox.domain.OutboxEventStatus;
import com.pointbank.ledger.outbox.mapper.OutboxEventMapper;
import com.pointbank.ledger.transfer.domain.LedgerTransferRequest;
import com.pointbank.ledger.transfer.domain.LedgerTransferStatus;
import com.pointbank.ledger.transfer.domain.LedgerTransferType;
import com.pointbank.ledger.transfer.mapper.LedgerTransferRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerOrderCommandDlqHandler {
    private static final String BUY_REQUEST_PREFIX = "STOCKBUY-";
    private static final String SELL_REQUEST_PREFIX = "STOCKSELL-";
    private static final String DLQ_REASON_CODE = "LEDGER_ORDER_COMMAND_DLQ";
    private static final String DLQ_REASON_MESSAGE =
            "Ledger order command moved to DLQ before funds debit.";

    private final LedgerTransferRequestMapper transferRequestMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(BuyOrderRequestedEvent event) {
        if (!LedgerEventType.BUY_ORDER_REQUESTED.equals(event.eventType())) {
            throw new IllegalArgumentException("Unsupported ledger order command DLQ event type.");
        }
        LedgerTransferRequest original = transferRequestMapper
                .findByRequestNoForUpdate(BUY_REQUEST_PREFIX + event.orderNo())
                .orElse(null);
        if (original != null && original.getStatus() == LedgerTransferStatus.COMPLETED) {
            ensureDebitedResult(event, original);
            return;
        }
        ensureCanceledResult(event);
    }

    @Transactional
    public void handle(SellOrderRequestedEvent event) {
        if (!LedgerEventType.SELL_ORDER_REQUESTED.equals(event.eventType())) {
            throw new IllegalArgumentException("Unsupported ledger sell command DLQ event type.");
        }
        LedgerTransferRequest original = transferRequestMapper
                .findByRequestNoForUpdate(SELL_REQUEST_PREFIX + event.orderNo()).orElse(null);
        if (original != null && original.getStatus() == LedgerTransferStatus.COMPLETED) {
            ensureCreditedResult(event, original);
            return;
        }
        ensureCanceledResult(event);
    }

    private void ensureCreditedResult(SellOrderRequestedEvent source, LedgerTransferRequest original) {
        boolean matches = original.getTransferType() == LedgerTransferType.SECURITIES_SELL
                && Objects.equals(original.getToMemberId(), source.memberId())
                && original.getAmount() == source.totalAmount()
                && original.getTargetBalanceAfter() != null;
        if (!matches) throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        if (outboxEventMapper.existsByEventTypeAndOrderNo(
                LedgerEventType.SELL_FUNDS_CREDITED, source.orderNo())) return;
        String eventId = UUID.randomUUID().toString();
        SellFundsCreditedEvent result = new SellFundsCreditedEvent(
                eventId, LedgerEventType.SELL_FUNDS_CREDITED, source.orderNo(), source.memberId(),
                source.securitiesAccountId(), source.stockCode(), source.stockName(), source.quantity(),
                source.orderPrice(), source.orderAmount(), source.fee(), source.tax(), source.totalAmount(),
                source.quoteObservedAt(), original.getRequestNo(), original.getTargetBalanceAfter());
        insertOutbox(source.securitiesAccountId(), eventId, LedgerEventType.SELL_FUNDS_CREDITED, result);
    }

    private void ensureCanceledResult(SellOrderRequestedEvent source) {
        if (outboxEventMapper.existsByEventTypeOrderNoAndReasonCode(
                LedgerEventType.SELL_FUNDS_FAILED, source.orderNo(), DLQ_REASON_CODE)) return;
        String eventId = UUID.randomUUID().toString();
        SellFundsFailedEvent result = new SellFundsFailedEvent(
                eventId, LedgerEventType.SELL_FUNDS_FAILED, source.orderNo(), source.memberId(),
                source.securitiesAccountId(), source.stockCode(), source.stockName(),
                DLQ_REASON_CODE, "Ledger sell order command moved to DLQ before funds credit.");
        insertOutbox(source.securitiesAccountId(), eventId, LedgerEventType.SELL_FUNDS_FAILED, result);
    }

    private void ensureDebitedResult(BuyOrderRequestedEvent source, LedgerTransferRequest original) {
        boolean matches = original.getTransferType() == LedgerTransferType.SECURITIES_BUY
                && Objects.equals(original.getFromMemberId(), source.memberId())
                && original.getAmount() == source.totalAmount()
                && original.getSourceBalanceAfter() != null;
        if (!matches) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (outboxEventMapper.existsByEventTypeAndOrderNo(
                LedgerEventType.BUY_FUNDS_DEBITED, source.orderNo())) {
            return;
        }
        String eventId = UUID.randomUUID().toString();
        BuyFundsDebitedEvent result = new BuyFundsDebitedEvent(
                eventId,
                LedgerEventType.BUY_FUNDS_DEBITED,
                source.orderNo(),
                source.memberId(),
                source.securitiesAccountId(),
                source.stockCode(),
                source.stockName(),
                source.quantity(),
                source.orderPrice(),
                source.orderAmount(),
                source.fee(),
                source.tax(),
                source.totalAmount(),
                source.quoteObservedAt(),
                original.getRequestNo(),
                original.getSourceBalanceAfter()
        );
        insertOutbox(source.securitiesAccountId(), eventId, LedgerEventType.BUY_FUNDS_DEBITED, result);
    }

    private void ensureCanceledResult(BuyOrderRequestedEvent source) {
        if (outboxEventMapper.existsByEventTypeOrderNoAndReasonCode(
                LedgerEventType.BUY_FUNDS_FAILED, source.orderNo(), DLQ_REASON_CODE)) {
            return;
        }
        String eventId = UUID.randomUUID().toString();
        BuyFundsFailedEvent result = new BuyFundsFailedEvent(
                eventId,
                LedgerEventType.BUY_FUNDS_FAILED,
                source.orderNo(),
                source.memberId(),
                source.securitiesAccountId(),
                source.stockCode(),
                source.stockName(),
                DLQ_REASON_CODE,
                DLQ_REASON_MESSAGE
        );
        insertOutbox(source.securitiesAccountId(), eventId, LedgerEventType.BUY_FUNDS_FAILED, result);
    }

    private void insertOutbox(Long aggregateId, String eventId, String eventType, Object payload) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent(
                    eventId,
                    eventType,
                    "SECURITIES_ORDER",
                    aggregateId,
                    objectMapper.writeValueAsString(payload),
                    OutboxEventStatus.PENDING
            );
            if (outboxEventMapper.insert(outboxEvent) != 1) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        } catch (JsonProcessingException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
