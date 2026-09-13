package com.mrdanissimo.shortener_service;

import com.mrdanissimo.shortener_service.entity.Link;
import com.mrdanissimo.shortener_service.entity.OutboxEvent;
import com.mrdanissimo.shortener_service.exception.LinkExpiredException;
import com.mrdanissimo.shortener_service.repository.LinkRepository;
import com.mrdanissimo.shortener_service.repository.OutboxEventRepository;
import com.mrdanissimo.shortener_service.service.LinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class LinkServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("shortener_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

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

    @Test
    void redirect_expiredLinkAfterCacheHit_throwsLinkExpiredException()
            throws InterruptedException {

        Link link = new Link();

        link.setShortCode("TEST-" + UUID.randomUUID());
        link.setOriginalUrl("https://example.com");
        link.setClicks(0L);
        link.setCreatedAt(LocalDateTime.now());

        // Ссылка будет действительна примерно 1 секунду
        link.setExpiresAt(LocalDateTime.now().plusSeconds(1));

        linkRepository.save(link);

        // Первый запрос:
        // CACHE MISS -> PostgreSQL -> Redis
        String originalUrl = linkService.redirect(
                link.getShortCode(),
                "test-agent"
        );

        assertEquals("https://example.com", originalUrl);

        // Ждём, пока expiresAt реально наступит
        Thread.sleep(1500);

        // Второй запрос:
        // CACHE HIT -> CachedLink из Redis -> проверка expiresAt -> exception
        assertThrows(
                LinkExpiredException.class,
                () -> linkService.redirect(
                        link.getShortCode(),
                        "test-agent"
                )
        );
    }
}