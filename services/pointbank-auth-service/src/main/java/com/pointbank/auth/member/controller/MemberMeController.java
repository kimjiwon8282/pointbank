package com.pointbank.auth.member.controller;

import com.pointbank.auth.global.response.ApiResponse;
import com.pointbank.auth.member.dto.MemberMeResponse;
import com.pointbank.auth.member.security.CustomUserDetails;
import com.pointbank.auth.member.service.MemberMeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/me")
@RequiredArgsConstructor
public class MemberMeController {

    private final MemberMeService memberMeService;

    @GetMapping
    public ApiResponse<MemberMeResponse> getMe(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MemberMeResponse response = memberMeService.getMember(userDetails.getMemberId());
        return ApiResponse.success("회원 정보 조회가 완료되었습니다.", response);
    }
}
