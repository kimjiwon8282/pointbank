package com.pointbank.auth.internal.dto;

import com.pointbank.auth.member.domain.MemberRole;
import com.pointbank.auth.member.domain.MemberStatus;

public record AuthValidateResponse(
        Long memberId,
        MemberRole role,
        MemberStatus status
) {
}
