package com.pointbank.securities.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BuyOrderRequest(
        @NotBlank @Size(max = 20) String stockCode,
        @Positive long quantity
) {
}
