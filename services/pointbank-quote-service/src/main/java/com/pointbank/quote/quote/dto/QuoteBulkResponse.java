package com.pointbank.quote.quote.dto;

import java.util.List;
import java.util.Map;

public record QuoteBulkResponse(
        Map<String, QuoteResponse> quotes,
        List<String> missingStockCodes,
        List<String> staleStockCodes
) {
}
