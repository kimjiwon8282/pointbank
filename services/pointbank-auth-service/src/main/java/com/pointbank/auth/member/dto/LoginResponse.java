package com.pointbank.auth.member.dto;

import com.pointbank.auth.member.domain.Member;
import com.pointbank.auth.member.domain.MemberRole;

public record LoginResponse(
        Long memberId,
        String name,
        String phoneNumber,
        MemberRole role,
        boolean simplePasswordSet,
        String accessToken,
        String refreshToken
) {
    public static LoginResponse of(Member member, String accessToken, String refreshToken) {
        return new LoginResponse(
                member.getId(),
                member.getName(),
                member.getPhoneNumber(),
                member.getRole(),
                member.isSimplePasswordSet(),
                accessToken,
                refreshToken
        );
    }
}
