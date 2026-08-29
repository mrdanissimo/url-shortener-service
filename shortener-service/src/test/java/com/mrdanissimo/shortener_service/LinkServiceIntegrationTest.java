package com.mrdanissimo.shortener_service;

import com.mrdanissimo.shortener_service.entity.Link;
import com.mrdanissimo.shortener_service.entity.OutboxEvent;
import com.mrdanissimo.shortener_service.repository.LinkRepository;
import com.mrdanissimo.shortener_service.repository.OutboxEventRepository;
import com.mrdanissimo.shortener_service.service.LinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class LinkServiceIntegrationTest {

    @Autowired
    private LinkService linkService;

    @Autowired
    private LinkRepository linkRepository;

    @SpyBean
    private OutboxEventRepository outboxRepository;

    @Test
    void redirect_rollback_noOutboxEventSaved() {

        // arrange
        Link link = new Link();
        link.setShortCode("TEST-" + UUID.randomUUID());
        link.setOriginalUrl("https://example.com");
        link.setClicks(0L);
        link.setCreatedAt(LocalDateTime.now());

        linkRepository.save(link);

        doThrow(new RuntimeException("Database error"))
                .when(outboxRepository)
                .save(any(OutboxEvent.class));

        // act
        assertThrows(
                RuntimeException.class,
                () -> linkService.redirect(
                        link.getShortCode(),
                        "test-agent"
                )
        );

        // assert
        Link result = linkRepository
                .findByShortCode(link.getShortCode())
                .orElseThrow();

        assertEquals(0L, result.getClicks());
    }
}