package com.mrdanissimo.shortener_service.service;

import com.mrdanissimo.shortener_service.entity.Link;
import com.mrdanissimo.shortener_service.exception.LinkExpiredException;
import com.mrdanissimo.shortener_service.exception.LinkNotFoundException;
import com.mrdanissimo.shortener_service.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LinkCacheService {

    private final LinkRepository linkRepository;

    @Cacheable(value = "originalUrls", key = "#shortCode")
    public String getOriginalUrl(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));

        if (link.getExpiresAt() != null
                && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new LinkExpiredException("Срок действия ссылки истек");
        }

        return link.getOriginalUrl();
    }
}