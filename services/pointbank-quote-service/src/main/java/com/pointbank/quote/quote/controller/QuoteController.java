package com.pointbank.quote.quote.controller;

import com.pointbank.quote.global.response.ApiResponse;
import com.pointbank.quote.quote.dto.MockQuoteSaveRequest;
import com.pointbank.quote.quote.dto.QuoteBulkRequest;
import com.pointbank.quote.quote.dto.QuoteBulkResponse;
import com.pointbank.quote.quote.dto.QuoteResponse;
import com.pointbank.quote.quote.service.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuoteController {
    private final QuoteService quoteService;

    /** Development-only ingestion endpoint until Korea Investment WebSocket integration is added. */
    @PostMapping("/internal/quotes/mock")
    public ApiResponse<QuoteResponse> saveMockQuote(@Valid @RequestBody MockQuoteSaveRequest request) {
        return ApiResponse.success("Mock quote saved.", quoteService.saveMockQuote(request));
    }

    @GetMapping("/internal/quotes/{stockCode}/latest")
    public ApiResponse<QuoteResponse> getInternalLatestQuote(@PathVariable String stockCode) {
        return ApiResponse.success("Latest quote retrieved.", quoteService.getLatestQuote(stockCode));
    }

    @GetMapping("/api/quotes/stocks/{stockCode}/latest")
    public ApiResponse<QuoteResponse> getLatestQuote(@PathVariable String stockCode) {
        return ApiResponse.success("Latest quote retrieved.", quoteService.getLatestQuote(stockCode));
    }

    @PostMapping("/internal/quotes/latest/bulk")
    public ApiResponse<QuoteBulkResponse> getLatestQuotes(@Valid @RequestBody QuoteBulkRequest request) {
        return ApiResponse.success("Latest quotes retrieved.", quoteService.getLatestQuotes(request));
    }
}
