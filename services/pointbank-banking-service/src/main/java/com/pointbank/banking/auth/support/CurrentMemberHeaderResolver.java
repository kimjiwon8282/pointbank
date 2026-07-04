package com.pointbank.banking.auth.support;

import com.pointbank.banking.auth.dto.BankingMeResponse;
import com.pointbank.banking.global.exception.CustomException;
import com.pointbank.banking.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class CurrentMemberHeaderResolver {

    public BankingMeResponse resolve(String memberIdHeader, String roleHeader) {
        if (memberIdHeader == null || memberIdHeader.isBlank()
                || roleHeader == null || roleHeader.isBlank()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        try {
            Long memberId = Long.valueOf(memberIdHeader.trim());
            return new BankingMeResponse(memberId, roleHeader.trim());
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }
}
