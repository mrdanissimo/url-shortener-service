package com.mrdanissimo.analytics_service.service;

import com.mrdanissimo.analytics_service.entity.ClickEvent;
import com.mrdanissimo.analytics_service.event.LinkClickedEvent;
import com.mrdanissimo.analytics_service.repository.ClickEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ClickEventRepository repository;

    public void saveClickEvent(LinkClickedEvent event) {
        if ("error".equals(event.shortCode())) {
            throw new RuntimeException("Simulated processing error for DLQ test");
        }

        ClickEvent entity = ClickEvent.builder()
                .shortCode(event.shortCode())
                .originalUrl(event.originalUrl())
                .clickedAt(event.clickedAt())
                .userAgent(event.userAgent())
                .correlationId(event.correlationId())
                .build();

        repository.save(entity);
        log.info("Successfully saved click event for shortCode: {}", event.shortCode());
    }
}
