package com.pointbank.quote.global.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pointbank.quote")
public record QuoteProperties(
        @Positive long ttlSeconds,
        @Positive long maxStaleSeconds
) {
}
