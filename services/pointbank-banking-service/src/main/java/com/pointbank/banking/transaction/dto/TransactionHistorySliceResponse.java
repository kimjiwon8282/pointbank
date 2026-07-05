package com.pointbank.banking.transaction.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TransactionHistorySliceResponse(
        List<TransactionHistoryItemResponse> items,
        boolean hasNext,
        LocalDateTime nextCursorCreatedAt,
        Long nextCursorId
) {
    public static TransactionHistorySliceResponse of(
            List<TransactionHistoryItemResponse> items, boolean hasNext) {
        TransactionHistoryItemResponse last = hasNext ? items.getLast() : null;
        return new TransactionHistorySliceResponse(
                items, hasNext,
                last == null ? null : last.createdAt(),
                last == null ? null : last.transactionId());
    }
}
