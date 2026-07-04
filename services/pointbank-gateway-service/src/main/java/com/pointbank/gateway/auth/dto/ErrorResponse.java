package com.pointbank.gateway.auth.dto;

import com.pointbank.gateway.auth.exception.ErrorCode;

public record ErrorResponse(
        boolean success,
        String code,
        String message
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage());
    }
}
