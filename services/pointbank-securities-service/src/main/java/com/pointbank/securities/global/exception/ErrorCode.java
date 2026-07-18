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
    SECURITIES_ACCOUNT_NOT_ACTIVE(
            HttpStatus.CONFLICT,
            "SECURITIES_ACCOUNT_NOT_ACTIVE",
            "Securities account is not active."
    ),
    INVALID_SECURITIES_ACCOUNT_PASSWORD(
            HttpStatus.BAD_REQUEST,
            "INVALID_SECURITIES_ACCOUNT_PASSWORD",
            "Securities account password is invalid."
    ),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product was not found."),
    PRODUCT_NOT_ACTIVE(HttpStatus.CONFLICT, "PRODUCT_NOT_ACTIVE", "Product is not active."),
    QUOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "QUOTE_NOT_FOUND", "Quote was not found."),
    STALE_QUOTE(HttpStatus.CONFLICT, "STALE_QUOTE", "Quote is stale."),
    QUOTE_SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "QUOTE_SERVICE_UNAVAILABLE",
            "Quote Service is unavailable."
    ),
    LEDGER_SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "LEDGER_SERVICE_UNAVAILABLE",
            "Ledger Service is unavailable."
    ),
    SECURITIES_CASH_ACCOUNT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SECURITIES_CASH_ACCOUNT_NOT_FOUND",
            "Securities cash account was not found."
    ),
    INSUFFICIENT_CASH_BALANCE(
            HttpStatus.CONFLICT,
            "INSUFFICIENT_CASH_BALANCE",
            "Securities cash balance is insufficient."
    ),
    INSUFFICIENT_HOLDING_QUANTITY(
            HttpStatus.CONFLICT,
            "INSUFFICIENT_HOLDING_QUANTITY",
            "Holding quantity is insufficient."
    ),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order was not found."),
    ORDER_FAILED(HttpStatus.CONFLICT, "ORDER_FAILED", "Order processing failed."),
    ORDER_IN_PROGRESS(HttpStatus.CONFLICT, "ORDER_IN_PROGRESS", "Order is in progress."),
    ORDER_MANUAL_REVIEW_REQUIRED(
            HttpStatus.CONFLICT,
            "ORDER_MANUAL_REVIEW_REQUIRED",
            "Order requires manual review."
    ),
    ORDER_IDEMPOTENCY_CONFLICT(
            HttpStatus.CONFLICT,
            "ORDER_IDEMPOTENCY_CONFLICT",
            "The idempotency key was used for a different order."
    ),
    OUTBOX_SAVE_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "OUTBOX_SAVE_FAILED",
            "Failed to save the order event."
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
