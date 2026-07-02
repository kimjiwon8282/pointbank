package com.pointbank.auth.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SimplePasswordSetupRequest(
        @NotBlank(message = "기기 식별자는 필수입니다.")
        @Size(min = 8, max = 100, message = "기기 식별자는 8자 이상 100자 이하여야 합니다.")
        String deviceId,

        @NotBlank(message = "간편 비밀번호는 필수입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "간편 비밀번호는 숫자 6자리여야 합니다.")
        String simplePassword,

        @NotBlank(message = "간편 비밀번호 확인은 필수입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "간편 비밀번호 확인은 숫자 6자리여야 합니다.")
        String confirmSimplePassword
) {
}
