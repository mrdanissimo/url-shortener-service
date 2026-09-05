package com.mrdanissimo.shortener_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrdanissimo.shortener_service.dto.CreateLinkRequest;
import com.mrdanissimo.shortener_service.dto.LinkResponse;
import com.mrdanissimo.shortener_service.entity.Link;
import com.mrdanissimo.shortener_service.event.LinkClickedEvent;
import com.mrdanissimo.shortener_service.exception.LinkNotFoundException;
import com.mrdanissimo.shortener_service.repository.LinkRepository;
import com.mrdanissimo.shortener_service.repository.OutboxEventRepository;
import com.mrdanissimo.shortener_service.service.LinkCacheService;
import com.mrdanissimo.shortener_service.service.LinkService;
import com.mrdanissimo.shortener_service.service.OutboxService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private LinkCacheService linkCacheService;

    @Mock
    private OutboxService outboxService;

    private LinkService linkService;

    private Link sampleLink;

    @BeforeEach
    void setUp() {
        linkService = new LinkService(
                linkRepository,
                linkCacheService,
                outboxService,
                new SimpleMeterRegistry()
        );

        sampleLink = new Link();
        sampleLink.setId(1L);
        sampleLink.setOriginalUrl("https://github.com");
        sampleLink.setShortCode("HLP0N0");
        sampleLink.setClicks(0L);
        sampleLink.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Создание ссылки сохраняет и возвращает response с правильными данными")
    void createLink_ShouldSaveAndReturnResponseDto() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setOriginalUrl("https://github.com");

        when(linkRepository.existsByShortCode(any())).thenReturn(false);
        when(linkRepository.save(any(Link.class))).thenAnswer(invocation -> {
            Link savedLink = invocation.getArgument(0);
            savedLink.setId(1L);
            return savedLink;
        });

        LinkResponse response = linkService.createLink(request);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalUrl()).isEqualTo("https://github.com");
        assertThat(response.getShortCode()).isNotNull().hasSize(6);
        assertThat(response.getClicks()).isEqualTo(0L);

        verify(linkRepository, times(1)).save(any(Link.class));
    }

    @Test
    @DisplayName("Поиск по существующему shortCode возвращает ссылку")
    void redirect_WhenCodeExists_ShouldReturnUrlAndIncrementClicks() {

        when(linkCacheService.getOriginalUrl("HLPON0"))
                .thenReturn("https://github.com");

        MDC.put("correlationId", "test-123");

        try {
            String originalUrl = linkService.redirect(
                    "HLPON0",
                    "Mozilla/5.0"
            );

            assertThat(originalUrl).isEqualTo("https://github.com");

            verify(linkRepository).incrementClicks("HLPON0");

            verify(outboxService).saveLinkClickedEvent(
                    any(LinkClickedEvent.class)
            );

        } finally {
            MDC.clear();
        }
    }

    @Test
    @DisplayName("Поиск по несуществующему shortCode бросает LinkNotFoundException")
    void redirect_WhenCodeDoesNotExist_ShouldThrowLinkNotFoundException() {
        when(linkCacheService.getOriginalUrl("UNKNOWN"))
                .thenThrow(new LinkNotFoundException("UNKNOWN"));

        MDC.put("correlationId", "test-123");

        try {
            assertThrows(
                    LinkNotFoundException.class,
                    () -> linkService.redirect(
                            "UNKNOWN",
                            "Mozilla/5.0"
                    )
            );
        } finally {
            MDC.clear();
        }
    }
}
