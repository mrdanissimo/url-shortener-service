package com.mrdanissimo.shortener_service;

import com.mrdanissimo.shortener_service.client.AnalyticsClient;
import com.mrdanissimo.shortener_service.dto.LinkAnalyticsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsClientTest {

    @Mock
    private WebClient analyticsWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private AnalyticsClient analyticsClient;

    @Test
    void getAnalytics_whenServiceUnavailable_returnsDefaultResponse() {

        when(analyticsWebClient.get())
                .thenReturn(requestHeadersUriSpec);

        when(requestHeadersUriSpec.uri(
                "/api/analytics/{shortCode}",
                "abc123"
        )).thenReturn(requestHeadersSpec);

        when(requestHeadersSpec.retrieve())
                .thenReturn(responseSpec);

        when(responseSpec.bodyToMono(LinkAnalyticsResponse.class))
                .thenReturn(Mono.error(
                        new RuntimeException("Analytics service unavailable")
                ));

        LinkAnalyticsResponse response =
                analyticsClient.getAnalytics("abc123");

        assertEquals("abc123", response.shortCode());
        assertEquals(0L, response.totalClicks());
    }
}