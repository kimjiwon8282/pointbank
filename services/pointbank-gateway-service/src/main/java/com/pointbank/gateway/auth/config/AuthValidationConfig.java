package com.pointbank.gateway.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AuthValidationProperties.class)
public class AuthValidationConfig {

    @Bean
    WebClient authValidationWebClient(WebClient.Builder builder) {
        return builder.build();
    }
}
