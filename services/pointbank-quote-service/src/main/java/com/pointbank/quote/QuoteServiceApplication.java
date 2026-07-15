package com.pointbank.quote;

import com.pointbank.quote.global.config.QuoteProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(QuoteProperties.class)
public class QuoteServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuoteServiceApplication.class, args);
    }
}
