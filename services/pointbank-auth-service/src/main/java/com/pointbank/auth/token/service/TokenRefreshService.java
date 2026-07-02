package com.pointbank.auth.token.service;

import com.pointbank.auth.global.exception.BusinessException;
import com.pointbank.auth.global.exception.ErrorCode;
import com.pointbank.auth.member.domain.Member;
import com.pointbank.auth.member.domain.MemberStatus;
import com.pointbank.auth.member.mapper.MemberMapper;
import com.pointbank.auth.member.security.CustomUserDetails;
import com.pointbank.auth.token.domain.RefreshToken;
import com.pointbank.auth.token.domain.RefreshTokenStatus;
import com.pointbank.auth.token.dto.TokenRefreshRequest;
import com.pointbank.auth.token.dto.TokenRefreshResponse;
import com.pointbank.auth.token.jwt.JwtProvider;
import com.pointbank.auth.token.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final RefreshTokenMapper refreshTokenMapper;
    private final MemberMapper memberMapper;
    private final JwtProvider jwtProvider;
    private final RefreshTokenHashService refreshTokenHashService;

    @Transactional
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        String rawRefreshToken = request.refreshToken();
        validateRefreshToken(rawRefreshToken);
        if (!jwtProvider.isRefreshToken(rawRefreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long memberId = jwtProvider.getMemberId(rawRefreshToken);
        String deviceId = jwtProvider.getDeviceId(rawRefreshToken);
        String tokenHash = refreshTokenHashService.hash(rawRefreshToken);

        RefreshToken storedToken = refreshTokenMapper.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (storedToken.getStatus() != RefreshTokenStatus.ACTIVE
                || !storedToken.getMemberId().equals(memberId)
                || !storedToken.getDeviceId().equals(deviceId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!storedToken.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        Member member = memberMapper.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_ACTIVE);
        }

        if (refreshTokenMapper.revokeByTokenHash(tokenHash, now) != 1) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newAccessToken = jwtProvider.createAccessToken(CustomUserDetails.from(member));
        String newRefreshToken = jwtProvider.createRefreshToken(memberId, deviceId);
        refreshTokenMapper.insert(new RefreshToken(
                memberId,
                deviceId,
                refreshTokenHashService.hash(newRefreshToken),
                RefreshTokenStatus.ACTIVE,
                jwtProvider.getExpirationAt(newRefreshToken)
        ));

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }

    private void validateRefreshToken(String refreshToken) {
        try {
            jwtProvider.validateToken(refreshToken);
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.EXPIRED_TOKEN) {
                throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
            }
            throw exception;
        }
    }
}
