package com.pointbank.banking.transfer.service;

import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class TransferNoGenerator {
    public String generate() {
        return "TRF" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
