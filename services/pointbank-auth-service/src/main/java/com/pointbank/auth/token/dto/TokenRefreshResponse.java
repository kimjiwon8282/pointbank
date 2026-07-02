package com.pointbank.auth.token.dto;

public record TokenRefreshResponse(String accessToken, String refreshToken) {
}
