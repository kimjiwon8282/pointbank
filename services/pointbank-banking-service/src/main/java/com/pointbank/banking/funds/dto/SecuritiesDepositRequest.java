package com.pointbank.banking.funds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record SecuritiesDepositRequest(
        @NotNull(message = "충전 금액은 필수입니다.")
        @Positive(message = "충전 금액은 양수여야 합니다.")
        Long amount,

        @NotBlank(message = "계좌 비밀번호는 필수입니다.")
        @Pattern(regexp = "^[0-9]{4}$", message = "계좌 비밀번호는 숫자 4자리여야 합니다.")
        String accountPassword
) {
}
