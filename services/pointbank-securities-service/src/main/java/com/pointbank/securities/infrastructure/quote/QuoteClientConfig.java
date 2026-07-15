package com.pointbank.securities.infrastructure.quote;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QuoteClientProperties.class)
public class QuoteClientConfig {
}
