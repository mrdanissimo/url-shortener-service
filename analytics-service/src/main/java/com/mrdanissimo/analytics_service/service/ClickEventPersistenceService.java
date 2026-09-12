package com.mrdanissimo.analytics_service.service;

import com.mrdanissimo.analytics_service.entity.ClickEvent;
import com.mrdanissimo.analytics_service.event.LinkClickedEvent;
import com.mrdanissimo.analytics_service.repository.ClickEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClickEventPersistenceService {

    private final ClickEventRepository repository;

    @Transactional
    public void save(LinkClickedEvent event) {
        ClickEvent entity = ClickEvent.builder()
                .eventId(event.eventId())
                .shortCode(event.shortCode())
                .originalUrl(event.originalUrl())
                .clickedAt(event.clickedAt())
                .userAgent(event.userAgent())
                .correlationId(event.correlationId())
                .build();

        repository.saveAndFlush(entity);
    }
}