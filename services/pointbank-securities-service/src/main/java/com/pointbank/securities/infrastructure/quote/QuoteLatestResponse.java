package com.pointbank.securities.infrastructure.quote;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuoteLatestResponse(
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
