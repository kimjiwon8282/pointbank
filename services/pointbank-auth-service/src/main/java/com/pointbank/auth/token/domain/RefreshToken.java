package com.pointbank.auth.token.domain;

import java.time.LocalDateTime;

public class RefreshToken {

    private Long id;
    private Long memberId;
    private String deviceId;
    private String tokenHash;
    private RefreshTokenStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;

    public RefreshToken() {
    }

    public RefreshToken(
            Long memberId,
            String deviceId,
            String tokenHash,
            RefreshTokenStatus status,
            LocalDateTime expiresAt
    ) {
        this.memberId = memberId;
        this.deviceId = deviceId;
        this.tokenHash = tokenHash;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getDeviceId() { return deviceId; }
    public String getTokenHash() { return tokenHash; }
    public RefreshTokenStatus getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
}
