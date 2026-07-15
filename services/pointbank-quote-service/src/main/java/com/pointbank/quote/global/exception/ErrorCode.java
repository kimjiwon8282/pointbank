package com.pointbank.quote.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "The request value is invalid."),
    QUOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "QUOTE_NOT_FOUND", "The latest quote is not available."),
    STALE_QUOTE(HttpStatus.CONFLICT, "STALE_QUOTE", "The latest quote is stale."),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An internal server error occurred."
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
