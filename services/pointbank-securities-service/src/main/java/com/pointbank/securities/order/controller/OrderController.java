package com.pointbank.securities.order.controller;

import com.pointbank.securities.auth.support.CurrentMemberHeaderResolver;
import com.pointbank.securities.global.response.ApiResponse;
import com.pointbank.securities.order.dto.BuyOrderRequest;
import com.pointbank.securities.order.dto.BuyOrderResponse;
import com.pointbank.securities.order.service.BuyOrderAcceptanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/securities/orders")
@RequiredArgsConstructor
public class OrderController {
    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final BuyOrderAcceptanceService buyOrderAcceptanceService;
    private final CurrentMemberHeaderResolver currentMemberHeaderResolver;

    @PostMapping("/buy")
    public ApiResponse<BuyOrderResponse> buy(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody BuyOrderRequest request
    ) {
        Long memberId = currentMemberHeaderResolver.resolveMemberId(memberIdHeader);
        return ApiResponse.success(
                "Buy order accepted.",
                buyOrderAcceptanceService.accept(memberId, idempotencyKey, request));
    }
}
