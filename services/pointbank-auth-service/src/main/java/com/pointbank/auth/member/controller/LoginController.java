package com.pointbank.auth.member.controller;

import com.pointbank.auth.global.response.ApiResponse;
import com.pointbank.auth.member.dto.LoginRequest;
import com.pointbank.auth.member.dto.LoginResponse;
import com.pointbank.auth.member.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/login")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @PostMapping
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("로그인이 완료되었습니다.", loginService.login(request));
    }
}
