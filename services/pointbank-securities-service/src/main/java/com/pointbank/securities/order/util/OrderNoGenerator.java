package com.pointbank.securities.order.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class OrderNoGenerator {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public String generateBuyOrderNo() {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return "ORD-BUY-" + timestamp + "-" + randomSuffix;
    }
}
