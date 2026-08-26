package com.mrdanissimo.shortener_service.service;

import com.mrdanissimo.shortener_service.dto.CreateLinkRequest;
import com.mrdanissimo.shortener_service.dto.LinkResponse;
import com.mrdanissimo.shortener_service.entity.Link;
import com.mrdanissimo.shortener_service.event.LinkClickedEvent;
import com.mrdanissimo.shortener_service.exception.LinkExpiredException;
import com.mrdanissimo.shortener_service.exception.LinkNotFoundException;
import com.mrdanissimo.shortener_service.repository.LinkRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Slf4j
public class LinkService {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrsABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final String TOPIC_LINK_CLICKS = "link-clicks";
    private final SecureRandom random = new SecureRandom();
    private final LinkRepository linkRepository;
    private final KafkaTemplate<String, LinkClickedEvent> kafkaTemplate;
    private final Counter linksClicksTotal;
    private final Counter linksCreatedTotal;
    private final Timer redirectTimer;

    public LinkService(
            LinkRepository linkRepository,
            KafkaTemplate<String, LinkClickedEvent> kafkaTemplate,
            MeterRegistry meterRegistry
    ) {
        this.linkRepository = linkRepository;
        this.kafkaTemplate = kafkaTemplate;

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
    // Генерация строки
    private String generateUniqueShortCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                int randomIndex = random.nextInt(ALPHABET.length());
                sb.append(ALPHABET.charAt(randomIndex));
            }
            code = sb.toString();
        } while (linkRepository.existsByShortCode(code));
        return code;
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

    @Cacheable(value = "links", key = "#shortCode")
    public LinkResponse getLinkInfo(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ссылка не найдена"));
        return mapToResponse(link);
    }

    @Transactional
    @CacheEvict(value = "links", key = "#shortCode")
    public void deleteLink(String shortCode) {
        if (!linkRepository.existsByShortCode(shortCode)) {
            throw new LinkNotFoundException(shortCode);
        }
        linkRepository.deleteByShortCode(shortCode);
    }

    @Cacheable(value = "links", key = "#shortCode")
    public String getOriginalUrl(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new LinkExpiredException("Срок действия ссылки истек");
        }

        return link.getOriginalUrl();
    }

    @Transactional
    public String redirect(String shortCode, String userAgent) {
        Timer.Sample sample = Timer.start();

        String originalUrl = getOriginalUrl(shortCode);

        linkRepository.incrementClicks(shortCode);

        linksClicksTotal.increment();

        String correlationId = MDC.get("correlationId");

        LinkClickedEvent event = new LinkClickedEvent(
                shortCode,
                originalUrl,
                LocalDateTime.now(),
                userAgent,
                correlationId
        );

        try {
            kafkaTemplate.send(
                    TOPIC_LINK_CLICKS,
                    shortCode,
                    event
            );

            log.info(
                    "Sent LinkClickedEvent to Kafka for shortCode: {}",
                    shortCode
            );

        } catch (Exception e) {
            log.error(
                    "Failed to send LinkClickedEvent to Kafka for shortCode: {}",
                    shortCode,
                    e
            );
        }

        sample.stop(redirectTimer);

        return originalUrl;
    }

    private Link findLinkByShortCode(String shortCode) {
        return linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));
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

    @Transactional
    public void incrementClicks(String shortCode) {
        linkRepository.incrementClicks(shortCode);
    }

}
