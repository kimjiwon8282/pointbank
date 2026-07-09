package com.pointbank.ledger.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TransferCreateRequest(
        @NotNull Long memberId,
        @NotBlank String toAccountNumber,
        @NotNull @Min(1) @Max(1_000_000) Long amount,
        @NotBlank @Pattern(regexp = "^[0-9]{4}$") String accountPassword
) {
}
