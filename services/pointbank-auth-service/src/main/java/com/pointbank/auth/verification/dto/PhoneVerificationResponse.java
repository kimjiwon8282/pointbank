package com.pointbank.auth.verification.dto;

import java.time.LocalDateTime;

public record PhoneVerificationResponse(String phoneNumber, LocalDateTime expiresAt) {
}
