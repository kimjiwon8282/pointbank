package com.pointbank.ledger.api.dto;

import jakarta.validation.constraints.NotNull;

public record SecuritiesCashAccountCreateRequest(@NotNull Long memberId) {
}
