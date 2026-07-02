package com.pointbank.banking.global.health;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banking")
public class BankingHealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "pointbank-banking-service", "status", "UP");
    }
}
