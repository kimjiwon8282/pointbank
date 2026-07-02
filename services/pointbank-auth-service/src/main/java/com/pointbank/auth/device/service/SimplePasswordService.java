package com.pointbank.auth.device.service;

import com.pointbank.auth.device.domain.MemberDevice;
import com.pointbank.auth.device.dto.SimplePasswordSetupRequest;
import com.pointbank.auth.device.dto.SimplePasswordSetupResponse;
import com.pointbank.auth.device.mapper.MemberDeviceMapper;
import com.pointbank.auth.global.exception.BusinessException;
import com.pointbank.auth.global.exception.ErrorCode;
import com.pointbank.auth.member.domain.Member;
import com.pointbank.auth.member.domain.MemberStatus;
import com.pointbank.auth.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SimplePasswordService {

    private final MemberMapper memberMapper;
    private final MemberDeviceMapper memberDeviceMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SimplePasswordSetupResponse setup(Long memberId, SimplePasswordSetupRequest request) {
        if (!request.simplePassword().equals(request.confirmSimplePassword())) {
            throw new BusinessException(ErrorCode.INVALID_SIMPLE_PASSWORD);
        }

        Member member = memberMapper.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_ACTIVE);
        }

        memberDeviceMapper.findByMemberIdAndDeviceId(memberId, request.deviceId())
                .orElseGet(() -> insertMemberDevice(memberId, request.deviceId()));

        memberDeviceMapper.updateSimplePassword(
                memberId,
                request.deviceId(),
                passwordEncoder.encode(request.simplePassword())
        );
        memberMapper.updateSimplePasswordSet(memberId, true);

        return new SimplePasswordSetupResponse(memberId, request.deviceId(), true);
    }

    private MemberDevice insertMemberDevice(Long memberId, String deviceId) {
        memberDeviceMapper.findByDeviceId(deviceId).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.INVALID_SIMPLE_PASSWORD);
        });

        MemberDevice memberDevice = new MemberDevice(memberId, deviceId);
        memberDeviceMapper.insert(memberDevice);
        return memberDevice;
    }
}
