package com.pointbank.banking.transaction.dto;

import com.pointbank.banking.transaction.domain.TransactionQueryType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionHistoryRequest(
        TransactionQueryType type,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
        Long cursorId,
        @Min(1) @Max(50) Integer size
) {
}
