package com.pointbank.auth.verification.service;

import com.pointbank.auth.global.exception.BusinessException;
import com.pointbank.auth.global.exception.ErrorCode;
import com.pointbank.auth.verification.domain.PhoneVerification;
import com.pointbank.auth.verification.dto.PhoneVerificationConfirmRequest;
import com.pointbank.auth.verification.dto.PhoneVerificationConfirmResponse;
import com.pointbank.auth.verification.dto.PhoneVerificationRequest;
import com.pointbank.auth.verification.dto.PhoneVerificationResponse;
import com.pointbank.auth.verification.mapper.PhoneVerificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final String MOCK_VERIFICATION_CODE = "123456";
    private static final long EXPIRATION_MINUTES = 5;

    private final PhoneVerificationMapper phoneVerificationMapper;

    @Transactional
    public PhoneVerificationResponse requestVerification(PhoneVerificationRequest request) {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
        PhoneVerification phoneVerification = new PhoneVerification(
                request.phoneNumber(),
                MOCK_VERIFICATION_CODE,
                expiresAt
        );
        phoneVerificationMapper.insert(phoneVerification);
        return new PhoneVerificationResponse(request.phoneNumber(), expiresAt);
    }

    @Transactional
    public PhoneVerificationConfirmResponse confirmVerification(PhoneVerificationConfirmRequest request) {
        PhoneVerification phoneVerification = phoneVerificationMapper
                .findLatestByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_VERIFICATION_NOT_FOUND));

        LocalDateTime verifiedAt = LocalDateTime.now();
        if (!phoneVerification.getExpiresAt().isAfter(verifiedAt)) {
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_EXPIRED);
        }
        if (!phoneVerification.getVerificationCode().equals(request.verificationCode())) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        phoneVerificationMapper.markVerified(phoneVerification.getId(), verifiedAt);
        return new PhoneVerificationConfirmResponse(request.phoneNumber(), true);
    }
}
