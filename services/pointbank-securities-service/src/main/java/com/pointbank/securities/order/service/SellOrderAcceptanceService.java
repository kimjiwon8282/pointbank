package com.pointbank.securities.order.service;

import com.pointbank.securities.account.domain.SecuritiesAccount;
import com.pointbank.securities.account.domain.SecuritiesAccountStatus;
import com.pointbank.securities.account.mapper.SecuritiesAccountMapper;
import com.pointbank.securities.global.exception.CustomException;
import com.pointbank.securities.global.exception.ErrorCode;
import com.pointbank.securities.infrastructure.quote.QuoteClient;
import com.pointbank.securities.infrastructure.quote.QuoteLatestResponse;
import com.pointbank.securities.order.domain.OrderSide;
import com.pointbank.securities.order.domain.OrderStatus;
import com.pointbank.securities.order.domain.SecuritiesOrder;
import com.pointbank.securities.order.dto.SellOrderRequest;
import com.pointbank.securities.order.dto.SellOrderResponse;
import com.pointbank.securities.order.mapper.SecuritiesOrderMapper;
import com.pointbank.securities.order.policy.TradingFeePolicy;
import com.pointbank.securities.order.util.OrderNoGenerator;
import com.pointbank.securities.product.domain.ProductStatus;
import com.pointbank.securities.product.domain.SecuritiesProduct;
import com.pointbank.securities.product.mapper.SecuritiesProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SellOrderAcceptanceService {
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 128;

    private final SecuritiesOrderMapper orderMapper;
    private final SecuritiesAccountMapper accountMapper;
    private final SecuritiesProductMapper productMapper;
    private final QuoteClient quoteClient;
    private final TradingFeePolicy tradingFeePolicy;
    private final OrderNoGenerator orderNoGenerator;
    private final OrderStateService orderStateService;
    private final PasswordEncoder securitiesAccountPasswordEncoder;

    public SellOrderResponse accept(Long memberId, String idempotencyKeyValue, SellOrderRequest request) {
        String idempotencyKey = normalizeIdempotencyKey(idempotencyKeyValue);
        String stockCode = normalizeStockCode(request.stockCode());
        SecuritiesAccount account = findActiveAccount(memberId);
        validateAccountPassword(account, request.accountPassword());

        SecuritiesOrder existing = orderMapper.findByMemberIdAndIdempotencyKey(memberId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            return resolveExistingOrder(existing, stockCode, request.quantity());
        }

        SecuritiesProduct product = findActiveProduct(stockCode);
        QuoteLatestResponse quote = quoteClient.getLatestQuote(stockCode);
        validateQuote(stockCode, quote);
        long orderAmount = multiply(quote.currentPrice(), request.quantity());
        long fee = tradingFeePolicy.calculateSellFee(orderAmount);
        long tax = tradingFeePolicy.calculateSellTax(orderAmount);
        long netAmount = subtract(subtract(orderAmount, fee), tax);
        if (netAmount <= 0L) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        SecuritiesOrder order = SecuritiesOrder.builder()
                .orderNo(orderNoGenerator.generateSellOrderNo())
                .idempotencyKey(idempotencyKey)
                .securitiesAccountId(account.getId())
                .memberId(memberId)
                .stockCode(stockCode)
                .orderSide(OrderSide.SELL)
                .quantity(request.quantity())
                .orderPrice(quote.currentPrice())
                .orderAmount(orderAmount)
                .fee(fee)
                .tax(tax)
                .totalAmount(netAmount)
                .quoteObservedAt(quote.observedAt())
                .status(OrderStatus.REQUESTED)
                .build();
        try {
            orderStateService.acceptSellOrder(order, product.getStockName());
        } catch (DuplicateKeyException exception) {
            SecuritiesOrder concurrent = orderMapper.findByMemberIdAndIdempotencyKey(memberId, idempotencyKey)
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
            return resolveExistingOrder(concurrent, stockCode, request.quantity());
        }
        return toResponse(order, product.getStockName(), "Sell order accepted.");
    }

    private SellOrderResponse resolveExistingOrder(SecuritiesOrder order, String stockCode, long quantity) {
        if (order.getOrderSide() != OrderSide.SELL
                || !order.getStockCode().equals(stockCode)
                || order.getQuantity() != quantity) {
            throw new CustomException(ErrorCode.ORDER_IDEMPOTENCY_CONFLICT);
        }
        String stockName = productMapper.findByStockCode(order.getStockCode())
                .map(SecuritiesProduct::getStockName).orElse(order.getStockCode());
        return switch (order.getStatus()) {
            case REQUESTED -> toResponse(order, stockName, "Sell order already accepted.");
            case COMPLETED -> toResponse(order, stockName, "Sell order already completed.");
            case FUNDS_COMPLETED -> throw new CustomException(ErrorCode.ORDER_IN_PROGRESS);
            case FAILED, CANCELED, REVERSED -> throw new CustomException(ErrorCode.ORDER_FAILED);
            case MANUAL_REVIEW -> throw new CustomException(ErrorCode.ORDER_MANUAL_REVIEW_REQUIRED);
        };
    }

    private SecuritiesAccount findActiveAccount(Long memberId) {
        SecuritiesAccount account = accountMapper.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SECURITIES_ACCOUNT_NOT_FOUND));
        if (account.getStatus() != SecuritiesAccountStatus.ACTIVE) {
            throw new CustomException(ErrorCode.SECURITIES_ACCOUNT_NOT_ACTIVE);
        }
        return account;
    }

    private void validateAccountPassword(SecuritiesAccount account, String password) {
        if (!securitiesAccountPasswordEncoder.matches(password, account.getAccountPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_SECURITIES_ACCOUNT_PASSWORD);
        }
    }

    private SecuritiesProduct findActiveProduct(String stockCode) {
        SecuritiesProduct product = productMapper.findByStockCode(stockCode)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_ACTIVE);
        }
        return product;
    }

    private void validateQuote(String stockCode, QuoteLatestResponse quote) {
        if (quote.stale()) throw new CustomException(ErrorCode.STALE_QUOTE);
        if (!stockCode.equals(quote.stockCode()) || quote.currentPrice() <= 0L || quote.observedAt() == null) {
            throw new CustomException(ErrorCode.QUOTE_SERVICE_UNAVAILABLE);
        }
    }

    private SellOrderResponse toResponse(SecuritiesOrder order, String stockName, String message) {
        return new SellOrderResponse(
                order.getOrderNo(), order.getStatus().name(), order.getStockCode(), stockName,
                order.getQuantity(), order.getOrderPrice(), order.getOrderAmount(), order.getFee(),
                order.getTax(), order.getTotalAmount(), order.getQuoteObservedAt(), message);
    }

    private String normalizeIdempotencyKey(String value) {
        String key = value == null ? null : value.trim();
        if (key == null || key.isEmpty() || key.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        return key;
    }

    private String normalizeStockCode(String value) {
        String code = value == null ? null : value.trim();
        if (code == null || code.isEmpty() || code.length() > 20) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        return code;
    }

    private long multiply(long left, long right) {
        try { return Math.multiplyExact(left, right); }
        catch (ArithmeticException exception) { throw new CustomException(ErrorCode.BAD_REQUEST); }
    }

    private long subtract(long left, long right) {
        try { return Math.subtractExact(left, right); }
        catch (ArithmeticException exception) { throw new CustomException(ErrorCode.BAD_REQUEST); }
    }
}
