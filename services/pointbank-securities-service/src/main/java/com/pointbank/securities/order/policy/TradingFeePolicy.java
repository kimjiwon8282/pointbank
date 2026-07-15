package com.pointbank.securities.order.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Simulated trading policy; rates can differ from a real brokerage.
 * Fractional KRW amounts are discarded with RoundingMode.DOWN.
 */
@Component
public class TradingFeePolicy {
    private static final BigDecimal BUY_FEE_RATE = new BigDecimal("0.00015");
    private static final BigDecimal SELL_FEE_RATE = new BigDecimal("0.00015");
    private static final BigDecimal SELL_TAX_RATE = new BigDecimal("0.0020");

    public long calculateBuyFee(long orderAmount) {
        return calculate(orderAmount, BUY_FEE_RATE);
    }

    public long calculateSellFee(long sellAmount) {
        return calculate(sellAmount, SELL_FEE_RATE);
    }

    public long calculateSellTax(long sellAmount) {
        return calculate(sellAmount, SELL_TAX_RATE);
    }

    private long calculate(long amount, BigDecimal rate) {
        if (amount < 0L) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        return BigDecimal.valueOf(amount)
                .multiply(rate)
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();
    }
}
