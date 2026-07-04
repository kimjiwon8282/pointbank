package com.pointbank.auth.internal.controller;

import com.pointbank.auth.global.response.ApiResponse;
import com.pointbank.auth.internal.dto.AuthValidateRequest;
import com.pointbank.auth.internal.dto.AuthValidateResponse;
import com.pointbank.auth.internal.service.InternalAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalAuthController {

    private final InternalAuthService internalAuthService;

    @PostMapping("/validate")
    public ApiResponse<AuthValidateResponse> validate(
            @Valid @RequestBody AuthValidateRequest request
    ) {
        return ApiResponse.success("토큰 검증이 완료되었습니다.", internalAuthService.validate(request));
    }
}
