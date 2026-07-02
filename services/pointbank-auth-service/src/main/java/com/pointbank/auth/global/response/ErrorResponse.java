package com.pointbank.auth.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pointbank.auth.global.exception.ErrorCode;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        boolean success,
        String code,
        String message,
        Map<String, String> validationErrors
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(false, errorCode.getCode(), message, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, Map<String, String> validationErrors) {
        return new ErrorResponse(
                false,
                errorCode.getCode(),
                errorCode.getMessage(),
                validationErrors
        );
    }
}
