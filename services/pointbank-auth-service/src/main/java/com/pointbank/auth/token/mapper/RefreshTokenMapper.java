package com.pointbank.auth.token.mapper;

import com.pointbank.auth.token.domain.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface RefreshTokenMapper {
    int insert(RefreshToken refreshToken);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    int revokeByTokenHash(@Param("tokenHash") String tokenHash, @Param("revokedAt") LocalDateTime revokedAt);
    int revokeActiveByMemberIdAndDeviceId(@Param("memberId") Long memberId, @Param("deviceId") String deviceId,
                                          @Param("revokedAt") LocalDateTime revokedAt);
}
