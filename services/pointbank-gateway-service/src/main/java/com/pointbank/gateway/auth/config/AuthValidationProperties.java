package com.pointbank.gateway.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "pointbank.auth")
public record AuthValidationProperties(
        String validateUrl,
        Duration timeout
) {
}
