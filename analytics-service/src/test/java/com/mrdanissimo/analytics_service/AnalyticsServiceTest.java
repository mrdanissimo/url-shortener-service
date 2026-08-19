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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

        verify(repository).save(captor.capture());

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

        verify(repository, times(2)).save(org.mockito.ArgumentMatchers.any(ClickEvent.class));
    }
}