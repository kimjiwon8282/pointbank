package com.pointbank.securities.infrastructure.quote;

import java.util.List;

public record QuoteBulkRequest(List<String> stockCodes) {
}
