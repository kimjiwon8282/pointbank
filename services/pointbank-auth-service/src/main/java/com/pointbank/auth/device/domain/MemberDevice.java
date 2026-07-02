package com.pointbank.auth.device.domain;

import java.time.LocalDateTime;

public class MemberDevice {

    private Long id;
    private Long memberId;
    private String deviceId;
    private String simplePasswordHash;
    private int failedCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MemberDevice() {
    }

    public MemberDevice(Long memberId, String deviceId) {
        this.memberId = memberId;
        this.deviceId = deviceId;
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getDeviceId() { return deviceId; }
    public String getSimplePasswordHash() { return simplePasswordHash; }
    public int getFailedCount() { return failedCount; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
