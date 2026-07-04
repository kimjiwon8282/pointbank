package com.pointbank.banking.account.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AccountDepositRequest(
        @NotNull(message = "충전 금액은 필수입니다.")
        @Min(value = 1, message = "충전 금액은 1 이상이어야 합니다.")
        @Max(value = 1_000_000, message = "1회 충전 한도를 초과했습니다.")
        Long amount
) {
}
