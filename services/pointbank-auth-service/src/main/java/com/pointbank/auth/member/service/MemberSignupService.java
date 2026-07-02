package com.pointbank.auth.member.service;

import com.pointbank.auth.global.exception.BusinessException;
import com.pointbank.auth.global.exception.ErrorCode;
import com.pointbank.auth.member.domain.Member;
import com.pointbank.auth.member.domain.MemberRole;
import com.pointbank.auth.member.domain.MemberStatus;
import com.pointbank.auth.member.dto.SignupRequest;
import com.pointbank.auth.member.dto.SignupResponse;
import com.pointbank.auth.member.mapper.MemberMapper;
import com.pointbank.auth.verification.domain.PhoneVerification;
import com.pointbank.auth.verification.mapper.PhoneVerificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberSignupService {

    private final MemberMapper memberMapper;
    private final PhoneVerificationMapper phoneVerificationMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (memberMapper.existsByPhoneNumber(request.phoneNumber())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        PhoneVerification phoneVerification = phoneVerificationMapper
                .findLatestByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_VERIFICATION_NOT_FOUND));

        if (!phoneVerification.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_EXPIRED);
        }
        if (!phoneVerification.isVerified()) {
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_NOT_COMPLETED);
        }

        Member member = new Member(
                request.name(),
                request.phoneNumber(),
                passwordEncoder.encode(request.password()),
                MemberRole.USER,
                MemberStatus.ACTIVE
        );

        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE_NUMBER);
        }
        return SignupResponse.from(member);
    }
}
