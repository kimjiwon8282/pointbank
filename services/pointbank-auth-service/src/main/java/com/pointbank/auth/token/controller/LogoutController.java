package com.pointbank.auth.token.controller;

import com.pointbank.auth.global.response.ApiResponse;
import com.pointbank.auth.member.security.CustomUserDetails;
import com.pointbank.auth.token.dto.LogoutRequest;
import com.pointbank.auth.token.service.LogoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/logout")
@RequiredArgsConstructor
public class LogoutController {

    private final LogoutService logoutService;

    @PostMapping
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LogoutRequest request
    ) {
        logoutService.logout(userDetails.getMemberId(), request);
        return ApiResponse.success("로그아웃이 완료되었습니다.", null);
    }
}
