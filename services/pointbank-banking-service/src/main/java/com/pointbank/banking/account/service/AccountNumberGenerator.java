package com.pointbank.banking.account.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final long MIN_ACCOUNT_NUMBER = 100_000_000_000L;
    private static final long ACCOUNT_NUMBER_RANGE = 900_000_000_000L;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        return Long.toString(MIN_ACCOUNT_NUMBER + secureRandom.nextLong(ACCOUNT_NUMBER_RANGE));
    }
}
