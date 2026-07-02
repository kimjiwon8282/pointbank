package com.pointbank.auth.verification.controller;

import com.pointbank.auth.global.response.ApiResponse;
import com.pointbank.auth.verification.dto.PhoneVerificationConfirmRequest;
import com.pointbank.auth.verification.dto.PhoneVerificationConfirmResponse;
import com.pointbank.auth.verification.dto.PhoneVerificationRequest;
import com.pointbank.auth.verification.dto.PhoneVerificationResponse;
import com.pointbank.auth.verification.service.PhoneVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/phone-verifications")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping
    public ApiResponse<PhoneVerificationResponse> requestVerification(
            @Valid @RequestBody PhoneVerificationRequest request
    ) {
        PhoneVerificationResponse response = phoneVerificationService.requestVerification(request);
        return ApiResponse.success("인증번호가 발송되었습니다.", response);
    }

    @PostMapping("/confirm")
    public ApiResponse<PhoneVerificationConfirmResponse> confirmVerification(
            @Valid @RequestBody PhoneVerificationConfirmRequest request
    ) {
        PhoneVerificationConfirmResponse response = phoneVerificationService.confirmVerification(request);
        return ApiResponse.success("본인확인이 완료되었습니다.", response);
    }
}
