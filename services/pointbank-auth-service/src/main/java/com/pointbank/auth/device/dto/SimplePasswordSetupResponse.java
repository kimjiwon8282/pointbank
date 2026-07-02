package com.pointbank.auth.device.dto;

public record SimplePasswordSetupResponse(Long memberId, String deviceId, boolean simplePasswordSet) {
}
