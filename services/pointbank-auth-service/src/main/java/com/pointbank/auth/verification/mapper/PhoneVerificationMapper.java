package com.pointbank.auth.verification.mapper;

import com.pointbank.auth.verification.domain.PhoneVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface PhoneVerificationMapper {
    int insert(PhoneVerification phoneVerification);
    Optional<PhoneVerification> findLatestByPhoneNumber(String phoneNumber);
    int markVerified(@Param("id") Long id, @Param("verifiedAt") LocalDateTime verifiedAt);
}
