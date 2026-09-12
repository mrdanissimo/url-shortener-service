package com.mrdanissimo.analytics_service;

import com.mrdanissimo.analytics_service.event.LinkClickedEvent;
import com.mrdanissimo.analytics_service.repository.ClickEventRepository;
import com.mrdanissimo.analytics_service.service.AnalyticsService;
import com.mrdanissimo.analytics_service.service.ClickEventPersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ClickEventRepository repository;

    @Mock
    private ClickEventPersistenceService persistenceService;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void saveClickEvent_savesEventToRepository() {

        UUID eventId = UUID.randomUUID();

        LinkClickedEvent event = new LinkClickedEvent(
                eventId,
                "abc123",
                "https://example.com",
                LocalDateTime.now(),
                "JUnit",
                "test-001"
        );

        analyticsService.saveClickEvent(event);

        verify(persistenceService).save(event);
    }

    @Test
    void saveClickEvent_sameShortCode_savesBothEvents() {

        UUID firstEventId = UUID.randomUUID();
        UUID secondEventId = UUID.randomUUID();

        LinkClickedEvent firstEvent = new LinkClickedEvent(
                firstEventId,
                "abc123",
                "https://example.com",
                LocalDateTime.now(),
                "JUnit",
                "test-001"
        );

        LinkClickedEvent secondEvent = new LinkClickedEvent(
                secondEventId,
                "abc123",
                "https://example.com",
                LocalDateTime.now(),
                "JUnit",
                "test-002"
        );

        analyticsService.saveClickEvent(firstEvent);
        analyticsService.saveClickEvent(secondEvent);

        verify(persistenceService, times(2))
                .save(any(LinkClickedEvent.class));
    }

    @Test
    void saveClickEvent_duplicateEventId_skipsEvent() {

        UUID eventId = UUID.randomUUID();

        LinkClickedEvent event = new LinkClickedEvent(
                eventId,
                "abc123",
                "https://example.com",
                LocalDateTime.now(),
                "JUnit",
                "duplicate-001"
        );

        when(repository.existsByEventId(eventId)).thenReturn(true);

        analyticsService.saveClickEvent(event);

        verify(persistenceService, never())
                .save(any(LinkClickedEvent.class));
    }

    @Test
    void saveClickEvent_sameCorrelationId_differentEventIds_savesBothEvents() {

        UUID firstEventId = UUID.randomUUID();
        UUID secondEventId = UUID.randomUUID();

        String correlationId = "same-correlation-id";

        LinkClickedEvent firstEvent = new LinkClickedEvent(
                firstEventId,
                "abc123",
                "https://example.com",
                LocalDateTime.now(),
                "JUnit",
                correlationId
        );

        LinkClickedEvent secondEvent = new LinkClickedEvent(
                secondEventId,
                "abc123",
                "https://example.com",
                LocalDateTime.now(),
                "JUnit",
                correlationId
        );

        analyticsService.saveClickEvent(firstEvent);
        analyticsService.saveClickEvent(secondEvent);

        verify(persistenceService, times(2))
                .save(any(LinkClickedEvent.class));
    }
}