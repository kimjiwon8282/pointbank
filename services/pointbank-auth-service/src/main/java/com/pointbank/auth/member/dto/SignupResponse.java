package com.pointbank.auth.member.dto;

import com.pointbank.auth.member.domain.Member;

public record SignupResponse(
        Long memberId,
        String name,
        String phoneNumber,
        boolean simplePasswordSet
) {
    public static SignupResponse from(Member member) {
        return new SignupResponse(
                member.getId(),
                member.getName(),
                member.getPhoneNumber(),
                member.isSimplePasswordSet()
        );
    }
}
