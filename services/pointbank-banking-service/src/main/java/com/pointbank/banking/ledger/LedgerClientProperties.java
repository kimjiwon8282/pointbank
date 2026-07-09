package com.pointbank.banking.ledger;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pointbank.ledger")
public record LedgerClientProperties(String baseUrl) {
}
