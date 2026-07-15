package com.pointbank.quote.quote.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MockQuoteSaveRequest(
        @NotBlank @Size(max = 20) String stockCode,
        @NotBlank @Size(max = 100) String stockName,
        @Positive long currentPrice,
        long changePrice,
        @NotNull @Digits(integer = 6, fraction = 4) BigDecimal changeRate,
        @PastOrPresent LocalDateTime observedAt
) {
}
