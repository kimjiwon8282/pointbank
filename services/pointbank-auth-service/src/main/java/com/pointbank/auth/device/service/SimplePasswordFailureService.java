package com.pointbank.auth.device.service;

import com.pointbank.auth.device.domain.MemberDevice;
import com.pointbank.auth.device.mapper.MemberDeviceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SimplePasswordFailureService {

    private static final int MAX_FAILED_COUNT = 5;
    private static final long LOCK_MINUTES = 5;

    private final MemberDeviceMapper memberDeviceMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(MemberDevice memberDevice) {
        memberDeviceMapper.incrementFailedCount(memberDevice.getId());
        MemberDevice updatedDevice = memberDeviceMapper.findByDeviceId(memberDevice.getDeviceId())
                .orElse(memberDevice);
        if (updatedDevice.getFailedCount() >= MAX_FAILED_COUNT) {
            memberDeviceMapper.lockUntil(
                    memberDevice.getId(),
                    LocalDateTime.now().plusMinutes(LOCK_MINUTES)
            );
        }
    }
}
