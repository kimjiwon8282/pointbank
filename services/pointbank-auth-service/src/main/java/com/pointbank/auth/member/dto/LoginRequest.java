package com.pointbank.auth.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        @Pattern(regexp = "^010\\d{8}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
        String phoneNumber,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(max = 72, message = "비밀번호는 72자 이하여야 합니다.")
        String password,

        @NotBlank(message = "기기 식별자는 필수입니다.")
        @Size(min = 8, max = 100, message = "기기 식별자는 8자 이상 100자 이하여야 합니다.")
        String deviceId
) {
}
