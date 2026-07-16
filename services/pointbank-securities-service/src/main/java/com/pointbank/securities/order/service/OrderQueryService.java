package com.pointbank.securities.order.service;

import com.pointbank.securities.execution.domain.SecuritiesExecution;
import com.pointbank.securities.execution.mapper.SecuritiesExecutionMapper;
import com.pointbank.securities.global.exception.CustomException;
import com.pointbank.securities.global.exception.ErrorCode;
import com.pointbank.securities.order.domain.SecuritiesOrder;
import com.pointbank.securities.order.dto.OrderDetailResponse;
import com.pointbank.securities.order.mapper.SecuritiesOrderMapper;
import com.pointbank.securities.product.domain.SecuritiesProduct;
import com.pointbank.securities.product.mapper.SecuritiesProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final SecuritiesOrderMapper orderMapper;
    private final SecuritiesExecutionMapper executionMapper;
    private final SecuritiesProductMapper productMapper;

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long memberId, String orderNoValue) {
        String orderNo = normalizeOrderNo(orderNoValue);
        SecuritiesOrder order = orderMapper.findByOrderNo(orderNo)
                .filter(found -> Objects.equals(found.getMemberId(), memberId))
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        SecuritiesExecution execution = executionMapper.findByOrderId(order.getId()).orElse(null);
        String stockName = productMapper.findByStockCode(order.getStockCode())
                .map(SecuritiesProduct::getStockName)
                .orElse(order.getStockCode());

        return new OrderDetailResponse(
                order.getOrderNo(),
                order.getStatus().name(),
                order.getStockCode(),
                stockName,
                order.getQuantity(),
                order.getOrderPrice(),
                order.getOrderAmount(),
                order.getFee(),
                order.getTax(),
                order.getTotalAmount(),
                order.getQuoteObservedAt(),
                order.getFailureReason(),
                order.getCompletedAt(),
                order.getCreatedAt(),
                execution == null ? null : execution.getExecutionPrice(),
                execution == null ? null : execution.getExecutedAt()
        );
    }

    private String normalizeOrderNo(String value) {
        String orderNo = value == null ? null : value.trim();
        if (orderNo == null || orderNo.isEmpty() || orderNo.length() > 64) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }
        return orderNo;
    }
}
