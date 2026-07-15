package com.pointbank.quote.quote.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LatestQuote(
        String stockCode,
        String stockName,
        long currentPrice,
        long changePrice,
        BigDecimal changeRate,
        LocalDateTime observedAt
) {
}
