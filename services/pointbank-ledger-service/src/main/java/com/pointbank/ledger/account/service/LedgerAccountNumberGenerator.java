package com.pointbank.ledger.account.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class LedgerAccountNumberGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        long value = Math.abs(RANDOM.nextLong()) % 1_000_000_000_000L;
        return String.format("%012d", value);
    }
}
