package com.pointbank.auth.member.dto;

import com.pointbank.auth.member.domain.Member;
import com.pointbank.auth.member.domain.MemberRole;
import com.pointbank.auth.member.domain.MemberStatus;

public record MemberMeResponse(
        Long memberId,
        String name,
        String phoneNumber,
        MemberRole role,
        MemberStatus status,
        boolean simplePasswordSet
) {
    public static MemberMeResponse from(Member member) {
        return new MemberMeResponse(
                member.getId(),
                member.getName(),
                member.getPhoneNumber(),
                member.getRole(),
                member.getStatus(),
                member.isSimplePasswordSet()
        );
    }
}
