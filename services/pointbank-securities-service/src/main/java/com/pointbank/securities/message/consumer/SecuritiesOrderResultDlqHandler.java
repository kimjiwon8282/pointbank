package com.pointbank.securities.message.consumer;

import com.pointbank.securities.event.BuyFundsDebitedEvent;
import com.pointbank.securities.event.BuyFundsFailedEvent;
import com.pointbank.securities.global.exception.CustomException;
import com.pointbank.securities.infrastructure.ledger.LedgerBuyReversalRequest;
import com.pointbank.securities.infrastructure.ledger.LedgerBuyReversalResponse;
import com.pointbank.securities.infrastructure.ledger.LedgerClient;
import com.pointbank.securities.message.mapper.ProcessedMessageMapper;
import com.pointbank.securities.order.domain.OrderSide;
import com.pointbank.securities.order.domain.OrderStatus;
import com.pointbank.securities.order.domain.SecuritiesOrder;
import com.pointbank.securities.order.service.OrderStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SecuritiesOrderResultDlqHandler {
    private static final String DLQ_EVENT_ID_PREFIX = "DLQ:";
    private static final String REVERSAL_REASON_CODE = "SECURITIES_ORDER_RESULT_DLQ";

    private final ProcessedMessageMapper processedMessageMapper;
    private final OrderStateService orderStateService;
    private final LedgerClient ledgerClient;
    private final PlatformTransactionManager transactionManager;

    public void handle(BuyFundsFailedEvent event) {
        transactionTemplate().executeWithoutResult(status -> cancelFailedResult(event));
    }

    public void handle(BuyFundsDebitedEvent event) {
        CompensationDecision decision = transactionTemplate().execute(status -> prepareCompensation(event));
        if (decision != CompensationDecision.COMPENSATE) {
            return;
        }

        try {
            LedgerBuyReversalResponse response = ledgerClient.reverseBuyFunds(new LedgerBuyReversalRequest(
                    event.memberId(),
                    event.orderNo(),
                    event.stockCode(),
                    event.totalAmount(),
                    event.ledgerRequestNo(),
                    REVERSAL_REASON_CODE,
                    "BUY_FUNDS_DEBITED could not be applied by Securities and moved to the result DLQ."
            ));
            validateReversalResponse(event, response);
            transactionTemplate().executeWithoutResult(status -> finishCompensation(event, response));
        } catch (Exception exception) {
            transactionTemplate().executeWithoutResult(status -> markCompensationFailure(event, exception));
        }
    }

    private void cancelFailedResult(BuyFundsFailedEvent event) {
        String dlqEventId = dlqEventId(event.eventId());
        if (processedMessageMapper.existsByEventId(dlqEventId)) {
            return;
        }
        SecuritiesOrder order = orderStateService.findOrderForResultProcessing(event.orderNo());
        String reason = "DLQ_CANCELED: " + failureReason(event.reasonCode(), event.reasonMessage());
        if (!matches(order, event)) {
            markManualReviewIfNeeded(order, "ORDER_RESULT_CONFLICT: BUY_FUNDS_FAILED DLQ payload mismatch.");
        } else {
            switch (order.getStatus()) {
                case REQUESTED, FAILED -> orderStateService.cancelOrderFromDlq(order, reason);
                case FUNDS_COMPLETED, COMPLETED -> markManualReviewIfNeeded(order, reason);
                case MANUAL_REVIEW, CANCELED, REVERSED -> { }
            }
        }
        recordProcessed(dlqEventId, event.eventType());
    }

    private CompensationDecision prepareCompensation(BuyFundsDebitedEvent event) {
        String dlqEventId = dlqEventId(event.eventId());
        if (processedMessageMapper.existsByEventId(dlqEventId)) {
            return CompensationDecision.DONE;
        }
        SecuritiesOrder order = orderStateService.findOrderForResultProcessing(event.orderNo());
        if (!matches(order, event)) {
            markManualReviewIfNeeded(order, "ORDER_RESULT_CONFLICT: BUY_FUNDS_DEBITED DLQ payload mismatch.");
            recordProcessed(dlqEventId, event.eventType());
            return CompensationDecision.DONE;
        }
        return switch (order.getStatus()) {
            case REQUESTED, FUNDS_COMPLETED -> CompensationDecision.COMPENSATE;
            case COMPLETED, REVERSED -> {
                recordProcessed(dlqEventId, event.eventType());
                yield CompensationDecision.DONE;
            }
            case FAILED, CANCELED -> {
                markManualReviewIfNeeded(order,
                        "ORDER_RESULT_CONFLICT: debited result reached DLQ after order termination.");
                recordProcessed(dlqEventId, event.eventType());
                yield CompensationDecision.DONE;
            }
            case MANUAL_REVIEW -> {
                recordProcessed(dlqEventId, event.eventType());
                yield CompensationDecision.DONE;
            }
        };
    }

    private void finishCompensation(BuyFundsDebitedEvent event, LedgerBuyReversalResponse response) {
        String dlqEventId = dlqEventId(event.eventId());
        if (processedMessageMapper.existsByEventId(dlqEventId)) {
            return;
        }
        SecuritiesOrder order = orderStateService.findOrderForResultProcessing(event.orderNo());
        String reason = "DLQ_REVERSED: reversalRequestNo=" + response.reversalRequestNo()
                + "; originalLedgerRequestNo=" + response.originalLedgerRequestNo();
        switch (order.getStatus()) {
            case REQUESTED, FUNDS_COMPLETED -> orderStateService.reverseOrderAfterCompensation(order, reason);
            case REVERSED -> { }
            case COMPLETED, FAILED, CANCELED -> markManualReviewIfNeeded(
                    order, "COMPENSATION_STATE_CONFLICT: " + reason);
            case MANUAL_REVIEW -> { }
        }
        recordProcessed(dlqEventId, event.eventType());
    }

    private void markCompensationFailure(BuyFundsDebitedEvent event, Exception exception) {
        String dlqEventId = dlqEventId(event.eventId());
        if (processedMessageMapper.existsByEventId(dlqEventId)) {
            return;
        }
        SecuritiesOrder order = orderStateService.findOrderForResultProcessing(event.orderNo());
        if (order.getStatus() != OrderStatus.COMPLETED
                && order.getStatus() != OrderStatus.REVERSED
                && order.getStatus() != OrderStatus.MANUAL_REVIEW) {
            String error = exception instanceof CustomException customException
                    ? customException.getErrorCode().getCode()
                    : exception.getClass().getSimpleName();
            orderStateService.markManualReviewAfterCompensationFailure(
                    order, "COMPENSATION_FAILED: " + error + "; originalLedgerRequestNo=" + event.ledgerRequestNo());
        }
        recordProcessed(dlqEventId, event.eventType());
    }

    private void markManualReviewIfNeeded(SecuritiesOrder order, String reason) {
        if (order.getStatus() != OrderStatus.MANUAL_REVIEW) {
            orderStateService.markManualReview(order.getId(), reason);
        }
    }

    private void validateReversalResponse(BuyFundsDebitedEvent event, LedgerBuyReversalResponse response) {
        if (response == null
                || !"COMPLETED".equals(response.status())
                || !Objects.equals(response.memberId(), event.memberId())
                || !Objects.equals(response.stockCode(), event.stockCode())
                || !Objects.equals(response.originalLedgerRequestNo(), event.ledgerRequestNo())
                || response.reversalAmount() != event.totalAmount()) {
            throw new IllegalStateException("Ledger returned an invalid buy reversal response.");
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

    private void recordProcessed(String dlqEventId, String eventType) {
        if (processedMessageMapper.insert(dlqEventId, "DLQ_" + eventType) != 1) {
            throw new IllegalStateException("Failed to record processed order result DLQ event.");
        }
    }

    private String dlqEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("DLQ eventId is required.");
        }
        return DLQ_EVENT_ID_PREFIX + eventId;
    }

    private String failureReason(String reasonCode, String reasonMessage) {
        return (reasonCode == null ? "BUY_FUNDS_FAILED" : reasonCode)
                + ": " + (reasonMessage == null ? "Ledger funds debit failed." : reasonMessage);
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private enum CompensationDecision {
        COMPENSATE,
        DONE
    }
}
