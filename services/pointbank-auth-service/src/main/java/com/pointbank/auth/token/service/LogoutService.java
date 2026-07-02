package com.pointbank.auth.token.service;

import com.pointbank.auth.global.exception.BusinessException;
import com.pointbank.auth.global.exception.ErrorCode;
import com.pointbank.auth.token.domain.RefreshToken;
import com.pointbank.auth.token.dto.LogoutRequest;
import com.pointbank.auth.token.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final RefreshTokenMapper refreshTokenMapper;
    private final RefreshTokenHashService refreshTokenHashService;

    @Transactional
    public void logout(Long memberId, LogoutRequest request) {
        String tokenHash = refreshTokenHashService.hash(request.refreshToken());
        Optional<RefreshToken> storedToken = refreshTokenMapper.findByTokenHash(tokenHash);
        if (storedToken.isEmpty()) {
            return;
        }

        RefreshToken refreshToken = storedToken.get();
        if (!refreshToken.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        refreshTokenMapper.revokeByTokenHash(tokenHash, LocalDateTime.now());
    }
}
