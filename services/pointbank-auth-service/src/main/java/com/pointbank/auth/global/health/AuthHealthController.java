package com.pointbank.auth.global.health;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthHealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "pointbank-auth-service", "status", "UP");
    }
}
