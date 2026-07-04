package com.pointbank.banking.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AccountCreateRequest(
        @NotBlank(message = "계좌 비밀번호는 필수입니다.")
        @Pattern(regexp = "^[0-9]{4}$", message = "계좌 비밀번호는 숫자 4자리여야 합니다.")
        String accountPassword
) {
}
