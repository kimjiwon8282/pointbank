package com.pointbank.ledger.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AccountDepositRequest(
        @NotNull Long memberId,
        @NotNull @Min(1) @Max(1_000_000) Long amount
) {
}
