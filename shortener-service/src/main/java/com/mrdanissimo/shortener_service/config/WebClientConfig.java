package com.mrdanissimo.shortener_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${analytics-service.url:http://localhost:8081}")
    private String analyticsServiceUrl;

    @Bean
    public WebClient analyticsWebClient() {
        return WebClient.builder()
                .baseUrl(analyticsServiceUrl)
                .build();
    }
}
