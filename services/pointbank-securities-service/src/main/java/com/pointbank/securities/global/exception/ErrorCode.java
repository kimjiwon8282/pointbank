package com.pointbank.securities.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    SECURITIES_ACCOUNT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SECURITIES_ACCOUNT_NOT_FOUND",
            "증권계좌를 찾을 수 없습니다."
    ),
    SECURITIES_ACCOUNT_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "SECURITIES_ACCOUNT_ALREADY_EXISTS",
            "이미 증권계좌가 존재합니다."
    ),
    LEDGER_SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "LEDGER_SERVICE_UNAVAILABLE",
            "Ledger Service is unavailable."
    ),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
