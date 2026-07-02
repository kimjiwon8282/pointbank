package com.pointbank.auth.token.controller;

import com.pointbank.auth.global.response.ApiResponse;
import com.pointbank.auth.token.dto.TokenRefreshRequest;
import com.pointbank.auth.token.dto.TokenRefreshResponse;
import com.pointbank.auth.token.service.TokenRefreshService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/token/refresh")
@RequiredArgsConstructor
public class TokenRefreshController {

    private final TokenRefreshService tokenRefreshService;

    @PostMapping
    public ApiResponse<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success("토큰이 재발급되었습니다.", tokenRefreshService.refresh(request));
    }
}
