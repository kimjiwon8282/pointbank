package com.pointbank.quote.quote.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.quote.global.exception.CustomException;
import com.pointbank.quote.global.exception.ErrorCode;
import com.pointbank.quote.global.config.QuoteProperties;
import com.pointbank.quote.quote.domain.LatestQuote;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QuoteCacheRepository {
    private static final String KEY_PREFIX = "quote:latest:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final QuoteProperties quoteProperties;

    public void save(LatestQuote quote) {
        try {
            String value = objectMapper.writeValueAsString(quote);
            redisTemplate.opsForValue().set(
                    key(quote.stockCode()), value, Duration.ofSeconds(quoteProperties.ttlSeconds()));
        } catch (JsonProcessingException | DataAccessException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public Optional<LatestQuote> findByStockCode(String stockCode) {
        try {
            String value = redisTemplate.opsForValue().get(key(stockCode));
            return value == null ? Optional.empty() : Optional.of(read(value));
        } catch (DataAccessException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, LatestQuote> findAllByStockCodes(List<String> stockCodes) {
        try {
            List<String> keys = stockCodes.stream().map(this::key).toList();
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            Map<String, LatestQuote> quotes = new LinkedHashMap<>();
            for (int index = 0; index < stockCodes.size(); index++) {
                String value = values.get(index);
                if (value != null) {
                    quotes.put(stockCodes.get(index), read(value));
                }
            }
            return quotes;
        } catch (DataAccessException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private LatestQuote read(String value) {
        try {
            return objectMapper.readValue(value, LatestQuote.class);
        } catch (JsonProcessingException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String key(String stockCode) {
        return KEY_PREFIX + stockCode;
    }
}
