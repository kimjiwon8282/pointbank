package com.pointbank.securities.message.consumer;

import com.pointbank.securities.event.BuyFundsDebitedEvent;
import com.pointbank.securities.event.BuyFundsFailedEvent;
import com.pointbank.securities.event.SecuritiesEventType;
import com.pointbank.securities.event.SellFundsCreditedEvent;
import com.pointbank.securities.event.SellFundsFailedEvent;
import com.pointbank.securities.message.mapper.ProcessedMessageMapper;
import com.pointbank.securities.order.domain.OrderSide;
import com.pointbank.securities.order.domain.OrderStatus;
import com.pointbank.securities.order.domain.SecuritiesOrder;
import com.pointbank.securities.order.service.OrderStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SecuritiesOrderResultHandler {
    private static final String RESULT_CONFLICT_PREFIX = "ORDER_RESULT_CONFLICT: ";
    private static final String LEDGER_ORDER_COMMAND_DLQ = "LEDGER_ORDER_COMMAND_DLQ";

    private final ProcessedMessageMapper processedMessageMapper;
    private final OrderStateService orderStateService;

    @Transactional
    public void handle(BuyFundsDebitedEvent event) {
        validateEvent(event.eventId(), event.eventType(), SecuritiesEventType.BUY_FUNDS_DEBITED);
        if (processedMessageMapper.existsByEventId(event.eventId())) {
            return;
        }

        SecuritiesOrder order = orderStateService.findOrderForResultProcessing(event.orderNo());
        if (!matches(order, event)) {
            completeAsManualReview(order, event.eventId(), event.eventType(),
                    RESULT_CONFLICT_PREFIX + "BUY_FUNDS_DEBITED payload does not match the order.");
            return;
        }

        switch (order.getStatus()) {
            case COMPLETED -> recordProcessed(event.eventId(), event.eventType());
            case FAILED -> completeAsManualReview(order, event.eventId(), event.eventType(),
                    RESULT_CONFLICT_PREFIX + "Funds were debited after the order failed.");
            case CANCELED -> completeAsManualReview(order, event.eventId(), event.eventType(),
                    RESULT_CONFLICT_PREFIX + "Funds were debited after the order was canceled.");
            case MANUAL_REVIEW, REVERSED -> recordProcessed(event.eventId(), event.eventType());
            case REQUESTED, FUNDS_COMPLETED -> {
                orderStateService.completeBuyOrderFromFundsDebited(order);
                recordProcessed(event.eventId(), event.eventType());
            }
        }
    }

    @Transactional
    public void handle(BuyFundsFailedEvent event) {
        validateEvent(event.eventId(), event.eventType(), SecuritiesEventType.BUY_FUNDS_FAILED);
        if (processedMessageMapper.existsByEventId(event.eventId())) {
            return;
        }

        SecuritiesOrder order = orderStateService.findOrderForResultProcessing(event.orderNo());
        if (!matches(order, event)) {
            completeAsManualReview(order, event.eventId(), event.eventType(),
                    RESULT_CONFLICT_PREFIX + "BUY_FUNDS_FAILED payload does not match the order.");
            return;
        }

        String failureReason = failureReason(event.reasonCode(), event.reasonMessage());
        boolean cancelFromDlq = LEDGER_ORDER_COMMAND_DLQ.equals(event.reasonCode());
        switch (order.getStatus()) {
            case REQUESTED -> {
                if (cancelFromDlq) {
                    orderStateService.cancelOrderFromDlq(order, failureReason);
                } else {
                    orderStateService.failBuyOrderFromFundsFailed(order, failureReason);
                }
                recordProcessed(event.eventId(), event.eventType());
            }
            case FAILED -> {
                if (cancelFromDlq) {
                    orderStateService.cancelOrderFromDlq(order, failureReason);
                }
                recordProcessed(event.eventId(), event.eventType());
            }
            case MANUAL_REVIEW, CANCELED, REVERSED -> recordProcessed(event.eventId(), event.eventType());
            case FUNDS_COMPLETED, COMPLETED -> completeAsManualReview(
                    order,
                    event.eventId(),
                    event.eventType(),
                    RESULT_CONFLICT_PREFIX + "Funds failure arrived after funds completion. " + failureReason
            );
        }
    }

    @Transactional
    public void handle(SellFundsCreditedEvent event) {
        validateEvent(event.eventId(), event.eventType(), SecuritiesEventType.SELL_FUNDS_CREDITED);
        if (processedMessageMapper.existsByEventId(event.eventId())) return;
        SecuritiesOrder order = orderStateService.findOrderForResultProcessing(event.orderNo());
        if (!matches(order, event)) {
            completeAsManualReview(order, event.eventId(), event.eventType(),
                    RESULT_CONFLICT_PREFIX + "SELL_FUNDS_CREDITED payload does not match the order.");
            return;
        }
        switch (order.getStatus()) {
            case COMPLETED -> recordProcessed(event.eventId(), event.eventType());
            case FAILED, CANCELED -> completeAsManualReview(
                    order, event.eventId(), event.eventType(),
                    RESULT_CONFLICT_PREFIX + "Sell funds were credited after order termination.");
            case MANUAL_REVIEW, REVERSED -> recordProcessed(event.eventId(), event.eventType());
            case REQUESTED, FUNDS_COMPLETED -> {
                orderStateService.completeSellOrderFromFundsCredited(order);
                recordProcessed(event.eventId(), event.eventType());
            }
        }
    }

    @Transactional
    public void handle(SellFundsFailedEvent event) {
        validateEvent(event.eventId(), event.eventType(), SecuritiesEventType.SELL_FUNDS_FAILED);
        if (processedMessageMapper.existsByEventId(event.eventId())) return;
        SecuritiesOrder order = orderStateService.findOrderForResultProcessing(event.orderNo());
        if (!matches(order, event)) {
            completeAsManualReview(order, event.eventId(), event.eventType(),
                    RESULT_CONFLICT_PREFIX + "SELL_FUNDS_FAILED payload does not match the order.");
            return;
        }
        String reason = failureReason(event.reasonCode(), event.reasonMessage());
        boolean cancelFromDlq = LEDGER_ORDER_COMMAND_DLQ.equals(event.reasonCode());
        switch (order.getStatus()) {
            case REQUESTED -> {
                if (cancelFromDlq) orderStateService.cancelSellOrderFromDlq(order, reason);
                else orderStateService.failSellOrderFromFundsFailed(order, reason);
                recordProcessed(event.eventId(), event.eventType());
            }
            case FAILED -> {
                if (cancelFromDlq) orderStateService.cancelOrderFromDlq(order, reason);
                recordProcessed(event.eventId(), event.eventType());
            }
            case MANUAL_REVIEW, CANCELED, REVERSED -> recordProcessed(event.eventId(), event.eventType());
            case FUNDS_COMPLETED, COMPLETED -> completeAsManualReview(
                    order, event.eventId(), event.eventType(),
                    RESULT_CONFLICT_PREFIX + "Sell funds failure arrived after credit. " + reason);
        }
    }

    private boolean matches(SecuritiesOrder order, BuyFundsDebitedEvent event) {
        return order.getOrderSide() == OrderSide.BUY
                && Objects.equals(order.getMemberId(), event.memberId())
                && Objects.equals(order.getSecuritiesAccountId(), event.securitiesAccountId())
                && Objects.equals(order.getStockCode(), event.stockCode())
                && order.getQuantity() == event.quantity()
                && order.getOrderPrice() == event.orderPrice()
                && order.getOrderAmount() == event.orderAmount()
                && order.getFee() == event.fee()
                && order.getTax() == event.tax()
                && order.getTotalAmount() == event.totalAmount();
    }

    private boolean matches(SecuritiesOrder order, BuyFundsFailedEvent event) {
        return order.getOrderSide() == OrderSide.BUY
                && Objects.equals(order.getMemberId(), event.memberId())
                && Objects.equals(order.getSecuritiesAccountId(), event.securitiesAccountId())
                && Objects.equals(order.getStockCode(), event.stockCode());
    }

    private boolean matches(SecuritiesOrder order, SellFundsCreditedEvent event) {
        return order.getOrderSide() == OrderSide.SELL
                && Objects.equals(order.getMemberId(), event.memberId())
                && Objects.equals(order.getSecuritiesAccountId(), event.securitiesAccountId())
                && Objects.equals(order.getStockCode(), event.stockCode())
                && order.getQuantity() == event.quantity()
                && order.getOrderPrice() == event.orderPrice()
                && order.getOrderAmount() == event.orderAmount()
                && order.getFee() == event.fee() && order.getTax() == event.tax()
                && order.getTotalAmount() == event.totalAmount();
    }

    private boolean matches(SecuritiesOrder order, SellFundsFailedEvent event) {
        return order.getOrderSide() == OrderSide.SELL
                && Objects.equals(order.getMemberId(), event.memberId())
                && Objects.equals(order.getSecuritiesAccountId(), event.securitiesAccountId())
                && Objects.equals(order.getStockCode(), event.stockCode());
    }

    private void completeAsManualReview(
            SecuritiesOrder order,
            String eventId,
            String eventType,
            String reason
    ) {
        // The conflicting result is consumed deliberately: retry cannot repair a semantic conflict,
        // and automatic Ledger compensation is outside this phase.
        if (order.getStatus() != OrderStatus.MANUAL_REVIEW) {
            orderStateService.markManualReview(order.getId(), reason);
        }
        recordProcessed(eventId, eventType);
    }

    private void recordProcessed(String eventId, String eventType) {
        if (processedMessageMapper.insert(eventId, eventType) != 1) {
            throw new IllegalStateException("Failed to record processed securities order result event.");
        }
    }

    private void validateEvent(String eventId, String actualEventType, String expectedEventType) {
        if (eventId == null || eventId.isBlank() || !expectedEventType.equals(actualEventType)) {
            throw new IllegalArgumentException("Invalid securities order result event.");
        }
    }

    private String failureReason(String reasonCode, String reasonMessage) {
        String code = reasonCode == null || reasonCode.isBlank() ? "BUY_FUNDS_FAILED" : reasonCode.trim();
        String message = reasonMessage == null || reasonMessage.isBlank()
                ? "Ledger rejected the buy funds debit."
                : reasonMessage.trim();
        return code + ": " + message;
    }
}
