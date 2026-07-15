package com.pointbank.ledger.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SecuritiesSellCreditRequest(
        @NotNull @Positive Long memberId,
        @NotBlank @Size(max = 54) String orderNo,
        @NotBlank @Size(max = 20) String stockCode,
        @Positive long sellAmount,
        @PositiveOrZero long fee,
        @PositiveOrZero long tax,
        @Positive long netAmount
) {
}
