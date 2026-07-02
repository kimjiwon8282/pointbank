package com.pointbank.auth.device.service;

import com.pointbank.auth.device.domain.MemberDevice;
import com.pointbank.auth.device.dto.SimpleLoginRequest;
import com.pointbank.auth.device.dto.SimpleLoginResponse;
import com.pointbank.auth.device.mapper.MemberDeviceMapper;
import com.pointbank.auth.global.exception.BusinessException;
import com.pointbank.auth.global.exception.ErrorCode;
import com.pointbank.auth.member.domain.Member;
import com.pointbank.auth.member.domain.MemberStatus;
import com.pointbank.auth.member.mapper.MemberMapper;
import com.pointbank.auth.member.security.CustomUserDetails;
import com.pointbank.auth.token.domain.RefreshToken;
import com.pointbank.auth.token.domain.RefreshTokenStatus;
import com.pointbank.auth.token.jwt.JwtProvider;
import com.pointbank.auth.token.mapper.RefreshTokenMapper;
import com.pointbank.auth.token.service.RefreshTokenHashService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SimpleLoginService {

    private final MemberDeviceMapper memberDeviceMapper;
    private final MemberMapper memberMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenHashService refreshTokenHashService;
    private final SimplePasswordFailureService simplePasswordFailureService;

    @Transactional
    public SimpleLoginResponse login(SimpleLoginRequest request) {
        MemberDevice memberDevice = memberDeviceMapper.findByDeviceId(request.deviceId())
                .filter(device -> device.getSimplePasswordHash() != null)
                .orElseThrow(() -> new BusinessException(ErrorCode.SIMPLE_PASSWORD_NOT_SET));

        LocalDateTime now = LocalDateTime.now();
        if (memberDevice.getLockedUntil() != null && memberDevice.getLockedUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_LOCKED);
        }

        if (!passwordEncoder.matches(request.simplePassword(), memberDevice.getSimplePasswordHash())) {
            simplePasswordFailureService.recordFailure(memberDevice);
            throw new BusinessException(ErrorCode.INVALID_SIMPLE_PASSWORD);
        }

        memberDeviceMapper.resetFailedCount(memberDevice.getId());
        Member member = memberMapper.findById(memberDevice.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_ACTIVE);
        }

        String accessToken = jwtProvider.createAccessToken(CustomUserDetails.from(member));
        String refreshToken = jwtProvider.createRefreshToken(member.getId(), memberDevice.getDeviceId());
        refreshTokenMapper.revokeActiveByMemberIdAndDeviceId(
                member.getId(),
                memberDevice.getDeviceId(),
                now
        );
        refreshTokenMapper.insert(new RefreshToken(
                member.getId(),
                memberDevice.getDeviceId(),
                refreshTokenHashService.hash(refreshToken),
                RefreshTokenStatus.ACTIVE,
                jwtProvider.getExpirationAt(refreshToken)
        ));

        return SimpleLoginResponse.of(member, accessToken, refreshToken);
    }
}
