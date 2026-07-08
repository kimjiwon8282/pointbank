package com.pointbank.securities.auth.support;

import com.pointbank.securities.global.exception.CustomException;
import com.pointbank.securities.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class CurrentMemberHeaderResolver {

    public Long resolveMemberId(String memberIdHeader) {
        if (memberIdHeader == null || memberIdHeader.isBlank()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(memberIdHeader.trim());
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }
}
