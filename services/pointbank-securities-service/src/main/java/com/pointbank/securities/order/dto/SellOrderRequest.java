package com.pointbank.securities.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SellOrderRequest(
        @NotBlank @Size(max = 20) String stockCode,
        @Positive long quantity,
        @NotBlank
        @Pattern(regexp = "^[0-9]{4}$", message = "Securities account password must be 4 digits.")
        String accountPassword
) {
}
