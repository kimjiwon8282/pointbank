package com.pointbank.securities.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.securities.event.BuyOrderRequestedEvent;
import com.pointbank.securities.event.SecuritiesEventType;
import com.pointbank.securities.execution.domain.ExecutionSide;
import com.pointbank.securities.execution.domain.SecuritiesExecution;
import com.pointbank.securities.execution.mapper.SecuritiesExecutionMapper;
import com.pointbank.securities.global.exception.CustomException;
import com.pointbank.securities.global.exception.ErrorCode;
import com.pointbank.securities.holding.domain.SecuritiesHolding;
import com.pointbank.securities.holding.mapper.SecuritiesHoldingMapper;
import com.pointbank.securities.order.domain.SecuritiesOrder;
import com.pointbank.securities.order.mapper.SecuritiesOrderMapper;
import com.pointbank.securities.outbox.domain.OutboxEvent;
import com.pointbank.securities.outbox.domain.OutboxEventStatus;
import com.pointbank.securities.outbox.mapper.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderStateService {
    private final SecuritiesOrderMapper orderMapper;
    private final SecuritiesExecutionMapper executionMapper;
    private final SecuritiesHoldingMapper holdingMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public SecuritiesOrder acceptBuyOrder(SecuritiesOrder order, String stockName) {
        if (orderMapper.insertRequested(order) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (outboxEventMapper.insert(createBuyOrderRequestedOutbox(order, stockName)) != 1) {
            throw new CustomException(ErrorCode.OUTBOX_SAVE_FAILED);
        }
        return order;
    }

    private OutboxEvent createBuyOrderRequestedOutbox(SecuritiesOrder order, String stockName) {
        String eventId = UUID.randomUUID().toString();
        BuyOrderRequestedEvent event = new BuyOrderRequestedEvent(
                eventId,
                SecuritiesEventType.BUY_ORDER_REQUESTED,
                order.getOrderNo(),
                order.getMemberId(),
                order.getSecuritiesAccountId(),
                order.getStockCode(),
                stockName,
                order.getQuantity(),
                order.getOrderPrice(),
                order.getOrderAmount(),
                order.getFee(),
                order.getTax(),
                order.getTotalAmount(),
                order.getQuoteObservedAt()
        );
        try {
            return new OutboxEvent(
                    eventId,
                    SecuritiesEventType.BUY_ORDER_REQUESTED,
                    "SECURITIES_ORDER",
                    order.getId(),
                    objectMapper.writeValueAsString(event),
                    OutboxEventStatus.PENDING
            );
        } catch (JsonProcessingException exception) {
            throw new CustomException(ErrorCode.OUTBOX_SAVE_FAILED);
        }
    }

    @Transactional
    public void markFundsCompleted(Long orderId) {
        if (orderMapper.markFundsCompleted(orderId) != 1) {
            throw new CustomException(ErrorCode.ORDER_IN_PROGRESS);
        }
    }

    @Transactional
    public void markFailed(Long orderId, String failureReason) {
        if (orderMapper.markFailed(orderId, truncateReason(failureReason)) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void markManualReview(Long orderId, String failureReason) {
        if (orderMapper.markManualReview(orderId, truncateReason(failureReason)) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public SecuritiesExecution completeBuyOrder(SecuritiesOrder order, LocalDateTime executedAt) {
        SecuritiesExecution execution = SecuritiesExecution.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .securitiesAccountId(order.getSecuritiesAccountId())
                .memberId(order.getMemberId())
                .stockCode(order.getStockCode())
                .executionSide(ExecutionSide.BUY)
                .executionPrice(order.getOrderPrice())
                .quantity(order.getQuantity())
                .executionAmount(order.getOrderAmount())
                .fee(order.getFee())
                .tax(0L)
                .buyCost(null)
                .realizedProfit(null)
                .realizedReturnRate(null)
                .executedAt(executedAt)
                .build();
        if (executionMapper.insert(execution) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        SecuritiesHolding holding = holdingMapper.findByAccountIdAndStockCodeForUpdate(
                order.getSecuritiesAccountId(), order.getStockCode()).orElse(null);
        if (holding == null) {
            insertHolding(order);
        } else {
            updateHoldingAfterBuy(holding, order);
        }

        if (orderMapper.markCompleted(order.getId(), executedAt) != 1) {
            throw new CustomException(ErrorCode.ORDER_IN_PROGRESS);
        }
        return execution;
    }

    private void insertHolding(SecuritiesOrder order) {
        BigDecimal avgBuyPrice = averagePrice(order.getOrderAmount(), order.getQuantity());
        SecuritiesHolding holding = SecuritiesHolding.builder()
                .securitiesAccountId(order.getSecuritiesAccountId())
                .memberId(order.getMemberId())
                .stockCode(order.getStockCode())
                .quantity(order.getQuantity())
                .reservedQuantity(0L)
                .avgBuyPrice(avgBuyPrice)
                .totalBuyAmount(order.getOrderAmount())
                .build();
        if (holdingMapper.insert(holding) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void updateHoldingAfterBuy(SecuritiesHolding holding, SecuritiesOrder order) {
        long quantityAfter = add(holding.getQuantity(), order.getQuantity());
        long totalBuyAmountAfter = add(holding.getTotalBuyAmount(), order.getOrderAmount());
        BigDecimal avgBuyPriceAfter = averagePrice(totalBuyAmountAfter, quantityAfter);
        if (holdingMapper.updateAfterBuy(
                holding.getId(), quantityAfter, avgBuyPriceAfter, totalBuyAmountAfter) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private BigDecimal averagePrice(long totalBuyAmount, long quantity) {
        return BigDecimal.valueOf(totalBuyAmount)
                .divide(BigDecimal.valueOf(quantity), 4, RoundingMode.DOWN);
    }

    private long add(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private String truncateReason(String failureReason) {
        String reason = failureReason == null || failureReason.isBlank()
                ? "Order processing failed."
                : failureReason;
        return reason.length() <= 255 ? reason : reason.substring(0, 255);
    }
}
