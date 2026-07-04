package com.pointbank.gateway.auth.dto;

public record AuthValidationResult(
        Long memberId,
        String role
) {
}
