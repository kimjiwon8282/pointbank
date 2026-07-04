package com.pointbank.banking.auth.dto;

public record BankingMeResponse(
        Long memberId,
        String role
) {
}
