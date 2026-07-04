package com.pointbank.auth.internal.service;

import com.pointbank.auth.global.exception.BusinessException;
import com.pointbank.auth.global.exception.ErrorCode;
import com.pointbank.auth.internal.dto.AuthValidateRequest;
import com.pointbank.auth.internal.dto.AuthValidateResponse;
import com.pointbank.auth.member.domain.Member;
import com.pointbank.auth.member.domain.MemberStatus;
import com.pointbank.auth.member.mapper.MemberMapper;
import com.pointbank.auth.token.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalAuthService {

    private final JwtProvider jwtProvider;
    private final MemberMapper memberMapper;

    @Transactional(readOnly = true)
    public AuthValidateResponse validate(AuthValidateRequest request) {
        String accessToken = request.accessToken();
        jwtProvider.validateToken(accessToken);
        if (!jwtProvider.isAccessToken(accessToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long memberId = jwtProvider.getMemberId(accessToken);
        Member member = memberMapper.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_ACTIVE);
        }

        return new AuthValidateResponse(member.getId(), member.getRole(), member.getStatus());
    }
}
