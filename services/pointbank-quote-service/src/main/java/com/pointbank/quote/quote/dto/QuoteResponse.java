package com.pointbank.quote.quote.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuoteResponse(
        String stockCode,
        String stockName,
        long currentPrice,
        long changePrice,
        BigDecimal changeRate,
        LocalDateTime observedAt,
        boolean stale,
        long ageSeconds
) {
}
