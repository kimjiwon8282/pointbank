package com.pointbank.banking.auth.support;

import com.pointbank.banking.auth.dto.BankingMeResponse;
import com.pointbank.banking.global.exception.CustomException;
import com.pointbank.banking.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class CurrentMemberHeaderResolver {

    public BankingMeResponse resolve(String memberIdHeader, String roleHeader) {
        if (roleHeader == null || roleHeader.isBlank()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long memberId = resolveMemberId(memberIdHeader);
        return new BankingMeResponse(memberId, roleHeader.trim());
    }

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
