package com.pointbank.auth.device.mapper;

import com.pointbank.auth.device.domain.MemberDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface MemberDeviceMapper {
    Optional<MemberDevice> findByDeviceId(String deviceId);
    Optional<MemberDevice> findByMemberIdAndDeviceId(@Param("memberId") Long memberId, @Param("deviceId") String deviceId);
    int insert(MemberDevice memberDevice);
    int updateSimplePassword(@Param("memberId") Long memberId, @Param("deviceId") String deviceId,
                             @Param("simplePasswordHash") String simplePasswordHash);
    int incrementFailedCount(Long id);
    int resetFailedCount(Long id);
    int lockUntil(@Param("id") Long id, @Param("lockedUntil") LocalDateTime lockedUntil);
}
