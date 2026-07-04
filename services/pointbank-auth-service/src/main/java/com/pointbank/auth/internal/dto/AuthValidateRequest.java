package com.pointbank.auth.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthValidateRequest(
        @NotBlank(message = "Access Token은 필수입니다.")
        String accessToken
) {
}
