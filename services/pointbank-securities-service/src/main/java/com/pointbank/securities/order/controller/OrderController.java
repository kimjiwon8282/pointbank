package com.pointbank.securities.order.controller;

import com.pointbank.securities.auth.support.CurrentMemberHeaderResolver;
import com.pointbank.securities.global.response.ApiResponse;
import com.pointbank.securities.order.dto.BuyOrderRequest;
import com.pointbank.securities.order.dto.BuyOrderResponse;
import com.pointbank.securities.order.dto.OrderDetailResponse;
import com.pointbank.securities.order.dto.SellOrderRequest;
import com.pointbank.securities.order.dto.SellOrderResponse;
import com.pointbank.securities.order.service.BuyOrderAcceptanceService;
import com.pointbank.securities.order.service.OrderQueryService;
import com.pointbank.securities.order.service.SellOrderAcceptanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final SellOrderAcceptanceService sellOrderAcceptanceService;
    private final OrderQueryService orderQueryService;
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

    @PostMapping("/sell")
    public ApiResponse<SellOrderResponse> sell(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody SellOrderRequest request
    ) {
        Long memberId = currentMemberHeaderResolver.resolveMemberId(memberIdHeader);
        return ApiResponse.success(
                "Sell order accepted.",
                sellOrderAcceptanceService.accept(memberId, idempotencyKey, request));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderDetailResponse> getOrder(
            @RequestHeader(value = MEMBER_ID_HEADER, required = false) String memberIdHeader,
            @PathVariable String orderNo
    ) {
        Long memberId = currentMemberHeaderResolver.resolveMemberId(memberIdHeader);
        return ApiResponse.success("Order retrieved.", orderQueryService.getOrder(memberId, orderNo));
    }
}
