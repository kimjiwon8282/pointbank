package com.pointbank.securities.infrastructure.quote;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pointbank.quote")
public record QuoteClientProperties(String baseUrl) {
}
