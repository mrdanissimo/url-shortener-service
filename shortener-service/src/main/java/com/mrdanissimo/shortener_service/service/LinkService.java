package com.mrdanissimo.shortener_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrdanissimo.shortener_service.dto.CreateLinkRequest;
import com.mrdanissimo.shortener_service.dto.LinkResponse;
import com.mrdanissimo.shortener_service.entity.Link;
import com.mrdanissimo.shortener_service.entity.OutboxEvent;
import com.mrdanissimo.shortener_service.event.LinkClickedEvent;
import com.mrdanissimo.shortener_service.exception.LinkExpiredException;
import com.mrdanissimo.shortener_service.exception.LinkNotFoundException;
import com.mrdanissimo.shortener_service.repository.LinkRepository;
import com.mrdanissimo.shortener_service.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class LinkService {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrsABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int CODE_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    private final LinkRepository linkRepository;
    private final LinkCacheService linkCacheService;
    private final OutboxService outboxService;

    private final Counter linksClicksTotal;
    private final Counter linksCreatedTotal;
    private final Timer redirectTimer;

    public LinkService(
            LinkRepository linkRepository,
            LinkCacheService linkCacheService,
            OutboxService outboxService,
            MeterRegistry meterRegistry
    ) {
        this.linkRepository = linkRepository;
        this.linkCacheService = linkCacheService;
        this.outboxService = outboxService;

        this.linksClicksTotal = Counter.builder("links.clicks.total")
                .description("Total number of link clicks")
                .register(meterRegistry);

        this.linksCreatedTotal = Counter.builder("links.created.total")
                .description("Total number of created links")
                .register(meterRegistry);

        this.redirectTimer = Timer.builder("links.redirect.time")
                .description("Time spent processing link redirects")
                .register(meterRegistry);
    }

    public LinkResponse createLink(CreateLinkRequest request) {
        String shortCode = generateUniqueShortCode();

        Link link = new Link();
        link.setOriginalUrl(request.getOriginalUrl());
        link.setShortCode(shortCode);
        link.setClicks(0L);
        link.setCreatedAt(LocalDateTime.now());

        Link savedLink = linkRepository.save(link);

        linksCreatedTotal.increment();

        return mapToResponse(savedLink);
    }

    public LinkResponse getStats(String shortCode) {
        return mapToResponse(findLinkByShortCode(shortCode));
    }

    @Cacheable(value = "linkInfo", key = "#shortCode")
    public LinkResponse getLinkInfo(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));

        return mapToResponse(link);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "linkInfo", key = "#shortCode"),
            @CacheEvict(value = "originalUrls", key = "#shortCode")
    })
    public void deleteLink(String shortCode) {
        if (!linkRepository.existsByShortCode(shortCode)) {
            throw new LinkNotFoundException(shortCode);
        }

        linkRepository.deleteByShortCode(shortCode);
    }

    @Transactional
    public String redirect(String shortCode, String userAgent) {
        Timer.Sample sample = Timer.start();

        try {
            String originalUrl = linkCacheService.getOriginalUrl(shortCode);

            incrementClicks(shortCode);
            linksClicksTotal.increment();

            log.info(
                    "Redirect link: shortCode={}, originalUrl={}",
                    shortCode,
                    originalUrl
            );

            LinkClickedEvent event = new LinkClickedEvent(
                    shortCode,
                    originalUrl,
                    LocalDateTime.now(),
                    userAgent,
                    MDC.get("correlationId")
            );

            outboxService.saveLinkClickedEvent(event);

            return originalUrl;

        } finally {
            sample.stop(redirectTimer);
        }
    }

    public void incrementClicks(String shortCode) {
        linkRepository.incrementClicks(shortCode);
    }

    private String generateUniqueShortCode() {
        String code;

        do {
            StringBuilder builder =
                    new StringBuilder(CODE_LENGTH);

            for (int i = 0; i < CODE_LENGTH; i++) {
                builder.append(
                        ALPHABET.charAt(
                                random.nextInt(ALPHABET.length())
                        )
                );
            }

            code = builder.toString();

        } while (linkRepository.existsByShortCode(code));

        return code;
    }

    private Link findLinkByShortCode(String shortCode) {
        return linkRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new LinkNotFoundException(shortCode)
                );
    }

    private LinkResponse mapToResponse(Link link) {
        return new LinkResponse(
                link.getId(),
                link.getOriginalUrl(),
                link.getShortCode(),
                link.getClicks(),
                link.getCreatedAt(),
                link.getExpiresAt()
        );
    }
}