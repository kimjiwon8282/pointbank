package com.pointbank.auth.device.controller;

import com.pointbank.auth.device.dto.SimpleLoginRequest;
import com.pointbank.auth.device.dto.SimpleLoginResponse;
import com.pointbank.auth.device.service.SimpleLoginService;
import com.pointbank.auth.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/simple-login")
@RequiredArgsConstructor
public class SimpleLoginController {

    private final SimpleLoginService simpleLoginService;

    @PostMapping
    public ApiResponse<SimpleLoginResponse> login(@Valid @RequestBody SimpleLoginRequest request) {
        return ApiResponse.success("간편 로그인이 완료되었습니다.", simpleLoginService.login(request));
    }
}
