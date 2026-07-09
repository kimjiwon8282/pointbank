package com.pointbank.ledger.global.response;

import com.pointbank.ledger.global.exception.ErrorCode;

public record ErrorResponse(
        boolean success,
        String code,
        String message
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage());
    }
}
