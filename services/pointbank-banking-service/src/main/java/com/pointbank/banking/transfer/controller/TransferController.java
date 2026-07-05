package com.pointbank.banking.transfer.controller;

import com.pointbank.banking.auth.support.CurrentMemberHeaderResolver;
import com.pointbank.banking.global.response.ApiResponse;
import com.pointbank.banking.transfer.dto.TransferCreateRequest;
import com.pointbank.banking.transfer.dto.TransferResponse;
import com.pointbank.banking.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banking/transfers")
@RequiredArgsConstructor
public class TransferController {
    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    private final TransferService transferService;
    private final CurrentMemberHeaderResolver currentMemberHeaderResolver;

    @PostMapping
    public ApiResponse<TransferResponse> transfer(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader,
            @Valid @RequestBody TransferCreateRequest request
    ) {
        Long memberId = currentMemberHeaderResolver.resolveMemberId(memberIdHeader);
        return ApiResponse.success("송금이 완료되었습니다.", transferService.transfer(memberId, request));
    }
}
