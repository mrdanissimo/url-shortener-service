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
    private final ClickEventPersistenceService persistenceService;

    public void saveClickEvent(LinkClickedEvent event) {

        if (event.eventId() == null) {
            throw new IllegalArgumentException("Click event must contain eventId");
        }

        if (repository.existsByEventId(event.eventId())) {
            log.info("Duplicate click event skipped: eventId={}", event.eventId());
            return;
        }

        try {
            persistenceService.save(event);

            log.info(
                    "Successfully saved click event for shortCode: {}",
                    event.shortCode()
            );

        } catch (DataIntegrityViolationException exception) {

            if (repository.existsByEventId(event.eventId())) {
                log.info(
                        "Duplicate click event skipped: eventId={}",
                        event.eventId()
                );
                return;
            }

            throw exception;
        }
    }
}
