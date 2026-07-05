package com.pointbank.banking.transfer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TransferCreateRequest(
        @NotBlank String toAccountNumber,
        @NotNull @Min(1) @Max(1_000_000) Long amount,
        @NotBlank
        @Pattern(regexp = "^[0-9]{4}$", message = "계좌 비밀번호는 숫자 4자리여야 합니다.")
        String accountPassword
) {
}
