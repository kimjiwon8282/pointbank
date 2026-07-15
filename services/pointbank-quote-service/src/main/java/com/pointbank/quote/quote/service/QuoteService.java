package com.pointbank.quote.quote.service;

import com.pointbank.quote.global.config.QuoteProperties;
import com.pointbank.quote.global.exception.CustomException;
import com.pointbank.quote.global.exception.ErrorCode;
import com.pointbank.quote.quote.cache.QuoteCacheRepository;
import com.pointbank.quote.quote.domain.LatestQuote;
import com.pointbank.quote.quote.dto.MockQuoteSaveRequest;
import com.pointbank.quote.quote.dto.QuoteBulkRequest;
import com.pointbank.quote.quote.dto.QuoteBulkResponse;
import com.pointbank.quote.quote.dto.QuoteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuoteService {
    private final QuoteCacheRepository quoteCacheRepository;
    private final QuoteProperties quoteProperties;

    // TODO: Replace mock ingestion with Korea Investment WebSocket connection,
    // stock subscription, Redis updates, and reconnect handling.

    public QuoteResponse saveMockQuote(MockQuoteSaveRequest request) {
        String stockCode = normalizeStockCode(request.stockCode());
        String stockName = request.stockName().trim();
        LocalDateTime observedAt = request.observedAt() == null ? LocalDateTime.now() : request.observedAt();
        LatestQuote quote = new LatestQuote(
                stockCode,
                stockName,
                request.currentPrice(),
                request.changePrice(),
                request.changeRate(),
                observedAt
        );
        quoteCacheRepository.save(quote);
        return toResponse(quote, LocalDateTime.now());
    }

    public QuoteResponse getLatestQuote(String stockCodeValue) {
        String stockCode = normalizeStockCode(stockCodeValue);
        LatestQuote quote = quoteCacheRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new CustomException(ErrorCode.QUOTE_NOT_FOUND));
        QuoteResponse response = toResponse(quote, LocalDateTime.now());
        if (response.stale()) {
            throw new CustomException(ErrorCode.STALE_QUOTE);
        }
        return response;
    }

    public QuoteBulkResponse getLatestQuotes(QuoteBulkRequest request) {
        List<String> stockCodes = new ArrayList<>(new LinkedHashSet<>(
                request.stockCodes().stream().map(this::normalizeStockCode).toList()));
        Map<String, LatestQuote> cachedQuotes = quoteCacheRepository.findAllByStockCodes(stockCodes);
        Map<String, QuoteResponse> quotes = new LinkedHashMap<>();
        List<String> missingStockCodes = new ArrayList<>();
        List<String> staleStockCodes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (String stockCode : stockCodes) {
            LatestQuote quote = cachedQuotes.get(stockCode);
            if (quote == null) {
                missingStockCodes.add(stockCode);
                continue;
            }
            QuoteResponse response = toResponse(quote, now);
            if (response.stale()) {
                staleStockCodes.add(stockCode);
                continue;
            }
            quotes.put(stockCode, response);
        }
        return new QuoteBulkResponse(quotes, missingStockCodes, staleStockCodes);
    }

    private QuoteResponse toResponse(LatestQuote quote, LocalDateTime now) {
        if (quote.observedAt() == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        Duration age = Duration.between(quote.observedAt(), now);
        if (age.isNegative()) {
            age = Duration.ZERO;
        }
        boolean stale = age.compareTo(Duration.ofSeconds(quoteProperties.maxStaleSeconds())) > 0;
        return new QuoteResponse(
                quote.stockCode(), quote.stockName(), quote.currentPrice(), quote.changePrice(),
                quote.changeRate(), quote.observedAt(), stale, age.getSeconds());
    }

    private String normalizeStockCode(String stockCodeValue) {
        String stockCode = stockCodeValue == null ? null : stockCodeValue.trim();
        if (stockCode == null || stockCode.isEmpty() || stockCode.length() > 20) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        return stockCode;
    }
}
