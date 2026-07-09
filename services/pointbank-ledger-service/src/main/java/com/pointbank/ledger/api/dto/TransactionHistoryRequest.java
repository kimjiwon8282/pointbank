package com.pointbank.ledger.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionHistoryRequest(
        Long memberId,
        String type,
        LocalDate from,
        LocalDate to,
        LocalDateTime cursorCreatedAt,
        Long cursorId,
        Integer size
) {
}
