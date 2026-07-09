package com.pointbank.ledger.global.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LedgerHealthController {
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
