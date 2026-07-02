package com.pointbank.auth.device.dto;

import com.pointbank.auth.member.domain.Member;
import com.pointbank.auth.member.domain.MemberRole;

public record SimpleLoginResponse(
        Long memberId,
        String name,
        String phoneNumber,
        MemberRole role,
        boolean simplePasswordSet,
        String accessToken,
        String refreshToken
) {
    public static SimpleLoginResponse of(Member member, String accessToken, String refreshToken) {
        return new SimpleLoginResponse(
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
