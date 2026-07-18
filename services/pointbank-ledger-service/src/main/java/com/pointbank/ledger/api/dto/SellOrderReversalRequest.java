package com.pointbank.ledger.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SellOrderReversalRequest(
        @NotNull @Positive Long memberId,
        @NotBlank @Size(max = 54) String orderNo,
        @NotBlank @Size(max = 20) String stockCode,
        @Positive long reversalAmount,
        @NotBlank @Size(max = 64) String originalLedgerRequestNo,
        @NotBlank @Size(max = 80) String reasonCode,
        @NotBlank @Size(max = 255) String reasonMessage
) {
}
