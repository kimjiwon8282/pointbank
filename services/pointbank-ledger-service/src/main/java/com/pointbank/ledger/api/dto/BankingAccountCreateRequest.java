package com.pointbank.ledger.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record BankingAccountCreateRequest(
        @NotNull Long memberId,
        @NotBlank
        @Pattern(regexp = "^[0-9]{4}$")
        String accountPassword
) {
}
