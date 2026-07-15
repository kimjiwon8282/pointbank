package com.pointbank.securities.infrastructure.quote;

import java.util.List;
import java.util.Map;

public record QuoteBulkResponse(
        Map<String, QuoteLatestResponse> quotes,
        List<String> missingStockCodes,
        List<String> staleStockCodes
) {
}
