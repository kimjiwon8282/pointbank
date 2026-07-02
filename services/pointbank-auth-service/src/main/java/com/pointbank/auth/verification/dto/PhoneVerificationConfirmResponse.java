package com.pointbank.auth.verification.dto;

public record PhoneVerificationConfirmResponse(String phoneNumber, boolean verified) {
}
