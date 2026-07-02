package com.pointbank.auth.verification.domain;

import java.time.LocalDateTime;

public class PhoneVerification {

    private Long id;
    private String phoneNumber;
    private String verificationCode;
    private boolean verified;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;

    public PhoneVerification() {
    }

    public PhoneVerification(String phoneNumber, String verificationCode, LocalDateTime expiresAt) {
        this.phoneNumber = phoneNumber;
        this.verificationCode = verificationCode;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getVerificationCode() { return verificationCode; }
    public boolean isVerified() { return verified; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
}
