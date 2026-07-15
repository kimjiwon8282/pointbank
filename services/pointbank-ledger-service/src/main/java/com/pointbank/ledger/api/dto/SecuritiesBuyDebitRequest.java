package com.pointbank.ledger.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SecuritiesBuyDebitRequest(
        @NotNull @Positive Long memberId,
        @NotBlank @Size(max = 54) String orderNo,
        @NotBlank @Size(max = 20) String stockCode,
        @Positive long orderAmount,
        @PositiveOrZero long fee,
        @Positive long totalAmount
) {
}
