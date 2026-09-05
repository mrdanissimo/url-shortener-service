package com.mrdanissimo.analytics_service.service;

import com.mrdanissimo.analytics_service.entity.ClickEvent;
import com.mrdanissimo.analytics_service.event.LinkClickedEvent;
import com.mrdanissimo.analytics_service.repository.ClickEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ClickEventRepository repository;

    public void saveClickEvent(LinkClickedEvent event) {
        if (event.correlationId() == null || event.correlationId().isBlank()) {
            throw new IllegalArgumentException("Click event must contain correlationId");
        }

        if (repository.existsByCorrelationId(event.correlationId())) {
            log.info("Duplicate click event skipped: correlationId={}", event.correlationId());
            return;
        }

        ClickEvent entity = ClickEvent.builder()
                .shortCode(event.shortCode())
                .originalUrl(event.originalUrl())
                .clickedAt(event.clickedAt())
                .userAgent(event.userAgent())
                .correlationId(event.correlationId())
                .build();

        try {
            repository.saveAndFlush(entity);
            log.info("Successfully saved click event for shortCode: {}", event.shortCode());
        } catch (DataIntegrityViolationException exception) {
            // A concurrent consumer may insert the same correlationId after the check above.
            if (repository.existsByCorrelationId(event.correlationId())) {
                log.info("Duplicate click event skipped: correlationId={}", event.correlationId());
                return;
            }

            throw exception;
        }
    }
}
