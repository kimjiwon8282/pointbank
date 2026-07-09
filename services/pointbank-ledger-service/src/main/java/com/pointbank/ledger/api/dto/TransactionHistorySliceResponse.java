package com.pointbank.ledger.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TransactionHistorySliceResponse(
        List<TransactionHistoryItemResponse> items,
        boolean hasNext,
        LocalDateTime nextCursorCreatedAt,
        Long nextCursorId
) {
}
