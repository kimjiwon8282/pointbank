package com.pointbank.auth.member.service;

import com.pointbank.auth.global.exception.BusinessException;
import com.pointbank.auth.global.exception.ErrorCode;
import com.pointbank.auth.member.domain.Member;
import com.pointbank.auth.member.domain.MemberStatus;
import com.pointbank.auth.member.dto.LoginRequest;
import com.pointbank.auth.member.dto.LoginResponse;
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
public class LoginService {

    private final MemberMapper memberMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenHashService refreshTokenHashService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberMapper.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_ACTIVE);
        }
        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        CustomUserDetails userDetails = CustomUserDetails.from(member);
        String accessToken = jwtProvider.createAccessToken(userDetails);
        String refreshToken = jwtProvider.createRefreshToken(member.getId(), request.deviceId());
        LocalDateTime now = LocalDateTime.now();

        refreshTokenMapper.revokeActiveByMemberIdAndDeviceId(member.getId(), request.deviceId(), now);
        refreshTokenMapper.insert(new RefreshToken(
                member.getId(),
                request.deviceId(),
                refreshTokenHashService.hash(refreshToken),
                RefreshTokenStatus.ACTIVE,
                jwtProvider.getExpirationAt(refreshToken)
        ));

        return LoginResponse.of(member, accessToken, refreshToken);
    }
}
