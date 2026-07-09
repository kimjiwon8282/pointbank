package com.pointbank.ledger.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "요청 값이 올바르지 않습니다."),
    ACCOUNT_ALREADY_EXISTS(HttpStatus.CONFLICT, "ACCOUNT_ALREADY_EXISTS", "이미 계좌가 존재합니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계좌를 찾을 수 없습니다."),
    ACCOUNT_NOT_ACTIVE(HttpStatus.CONFLICT, "ACCOUNT_NOT_ACTIVE", "사용할 수 없는 계좌 상태입니다."),
    INVALID_ACCOUNT_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT_PASSWORD", "계좌 비밀번호가 일치하지 않습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", "계좌 잔액이 부족합니다."),
    INVALID_TRANSFER_TARGET(HttpStatus.BAD_REQUEST, "INVALID_TRANSFER_TARGET", "송금 대상이 올바르지 않습니다."),
    CANNOT_TRANSFER_TO_SELF(HttpStatus.BAD_REQUEST, "CANNOT_TRANSFER_TO_SELF", "본인 계좌로 송금할 수 없습니다."),
    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", "이미 다른 요청으로 사용된 멱등키입니다."),
    FUND_TRANSFER_IN_PROGRESS(HttpStatus.CONFLICT, "FUND_TRANSFER_IN_PROGRESS", "자금 이동 요청이 처리 중입니다."),
    SECURITIES_CASH_ACCOUNT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SECURITIES_CASH_ACCOUNT_NOT_FOUND",
            "증권 예수금 계좌를 찾을 수 없습니다."
    ),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
