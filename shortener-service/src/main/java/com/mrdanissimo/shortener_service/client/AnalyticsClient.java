package com.mrdanissimo.shortener_service.client;

import com.mrdanissimo.shortener_service.dto.LinkAnalyticsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsClient {

    private final WebClient analyticsWebClient;

    public LinkAnalyticsResponse getAnalytics(String shortCode) {
        return analyticsWebClient.get()
                .uri("/api/analytics/{shortCode}", shortCode)
                .retrieve()
                .bodyToMono(LinkAnalyticsResponse.class)
                .timeout(Duration.ofSeconds(2))
                .onErrorResume(e -> {
                    log.error("Analytics service is unavailable for shortCode: {}. Returning default fallback.", shortCode, e);
                    return Mono.just(new LinkAnalyticsResponse(shortCode, 0L));
                })
                .block();
    }
}
