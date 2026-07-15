package com.pointbank.securities.infrastructure.quote;

public record QuoteApiResponse<T>(
        boolean success,
        String message,
        T data
) {
}
