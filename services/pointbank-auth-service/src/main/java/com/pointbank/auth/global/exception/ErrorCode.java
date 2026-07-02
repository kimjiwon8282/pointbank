package com.pointbank.auth.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    MEMBER_NOT_ACTIVE(HttpStatus.FORBIDDEN, "MEMBER_NOT_ACTIVE", "이용할 수 없는 회원 상태입니다."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "DUPLICATE_PHONE_NUMBER", "이미 가입된 휴대폰 번호입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "비밀번호가 올바르지 않습니다."),

    PHONE_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PHONE_VERIFICATION_NOT_FOUND", "본인확인 요청을 찾을 수 없습니다."),
    PHONE_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "PHONE_VERIFICATION_EXPIRED", "본인확인 요청이 만료되었습니다."),
    PHONE_VERIFICATION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "PHONE_VERIFICATION_NOT_COMPLETED", "본인확인이 완료되지 않았습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE", "인증번호가 올바르지 않습니다."),

    SIMPLE_PASSWORD_NOT_SET(HttpStatus.BAD_REQUEST, "SIMPLE_PASSWORD_NOT_SET", "간편 비밀번호가 설정되지 않았습니다."),
    INVALID_SIMPLE_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_SIMPLE_PASSWORD", "간편 비밀번호가 올바르지 않습니다."),
    SIMPLE_PASSWORD_LOCKED(HttpStatus.LOCKED, "SIMPLE_PASSWORD_LOCKED", "간편 비밀번호 입력이 잠겼습니다."),

    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "REFRESH_TOKEN_NOT_FOUND", "Refresh Token을 찾을 수 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN", "만료된 Refresh Token입니다.");

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
