package com.mrdanissimo.analytics_service;

import com.mrdanissimo.analytics_service.entity.ClickEvent;
import com.mrdanissimo.analytics_service.event.LinkClickedEvent;
import com.mrdanissimo.analytics_service.repository.ClickEventRepository;
import com.mrdanissimo.analytics_service.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ClickEventRepository repository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void saveClickEvent_savesEventToRepository() {
        LinkClickedEvent event = new LinkClickedEvent(
                "abc123",
                "https://example.com",
                LocalDateTime.now(),
                "JUnit",
                "test-001"
        );

        analyticsService.saveClickEvent(event);

        ArgumentCaptor<ClickEvent> captor =
                ArgumentCaptor.forClass(ClickEvent.class);

        verify(repository).saveAndFlush(captor.capture());

        ClickEvent savedEvent = captor.getValue();

        assertEquals("abc123", savedEvent.getShortCode());
        assertEquals("https://example.com", savedEvent.getOriginalUrl());
        assertEquals("JUnit", savedEvent.getUserAgent());
        assertEquals("test-001", savedEvent.getCorrelationId());
    }

    @Test
    void saveClickEvent_sameShortCode_savesBothEvents() {
        LinkClickedEvent firstEvent = new LinkClickedEvent(
                "abc123",
                "https://example.com",
                LocalDateTime.now(),
                "JUnit",
                "test-001"
        );

        LinkClickedEvent secondEvent = new LinkClickedEvent(
                "abc123",
                "https://example.com",
                LocalDateTime.now(),
                "JUnit",
                "test-002"
        );

        analyticsService.saveClickEvent(firstEvent);
        analyticsService.saveClickEvent(secondEvent);

        verify(repository, times(2)).saveAndFlush(any(ClickEvent.class));
    }

    @Test
    void saveClickEvent_duplicateCorrelationId_skipsEvent() {
        LinkClickedEvent event = new LinkClickedEvent(
                "abc123",
                "https://example.com",
                LocalDateTime.now(),
                "JUnit",
                "duplicate-001"
        );
        when(repository.existsByCorrelationId("duplicate-001")).thenReturn(true);

        analyticsService.saveClickEvent(event);

        verify(repository, never()).saveAndFlush(any(ClickEvent.class));
    }
}
