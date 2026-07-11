package com.pointbank.securities.infrastructure.ledger;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pointbank.ledger")
public record LedgerClientProperties(String baseUrl) {
}
