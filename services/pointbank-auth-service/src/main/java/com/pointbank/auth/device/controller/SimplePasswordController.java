package com.pointbank.auth.device.controller;

import com.pointbank.auth.device.dto.SimplePasswordSetupRequest;
import com.pointbank.auth.device.dto.SimplePasswordSetupResponse;
import com.pointbank.auth.device.service.SimplePasswordService;
import com.pointbank.auth.global.response.ApiResponse;
import com.pointbank.auth.member.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/simple-password")
@RequiredArgsConstructor
public class SimplePasswordController {

    private final SimplePasswordService simplePasswordService;

    @PostMapping
    public ApiResponse<SimplePasswordSetupResponse> setup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SimplePasswordSetupRequest request
    ) {
        SimplePasswordSetupResponse response = simplePasswordService.setup(
                userDetails.getMemberId(),
                request
        );
        return ApiResponse.success("간편 비밀번호가 설정되었습니다.", response);
    }
}
