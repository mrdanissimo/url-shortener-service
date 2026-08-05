package com.mrdanissimo.shortener_service.service;

import com.mrdanissimo.shortener_service.dto.CreateLinkRequest;
import com.mrdanissimo.shortener_service.dto.LinkResponse;
import com.mrdanissimo.shortener_service.entity.Link;
import com.mrdanissimo.shortener_service.exception.LinkExpiredException;
import com.mrdanissimo.shortener_service.exception.LinkNotFoundException;
import com.mrdanissimo.shortener_service.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LinkService {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrsABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();
    private final LinkRepository linkRepository;

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

    public String redirect(String shortCode) {
        String originalUrl = getOriginalUrl(shortCode);

        linkRepository.incrementClicks(shortCode);
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
