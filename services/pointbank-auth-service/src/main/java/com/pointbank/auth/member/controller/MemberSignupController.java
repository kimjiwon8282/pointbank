package com.pointbank.auth.member.controller;

import com.pointbank.auth.global.response.ApiResponse;
import com.pointbank.auth.member.dto.SignupRequest;
import com.pointbank.auth.member.dto.SignupResponse;
import com.pointbank.auth.member.service.MemberSignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/signup")
@RequiredArgsConstructor
public class MemberSignupController {

    private final MemberSignupService memberSignupService;

    @PostMapping
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = memberSignupService.signup(request);
        return ApiResponse.success("회원가입이 완료되었습니다.", response);
    }
}
