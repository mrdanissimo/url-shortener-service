package com.mrdanissimo.shortener_service.service;

import com.mrdanissimo.shortener_service.dto.CachedLink;
import com.mrdanissimo.shortener_service.entity.Link;
import com.mrdanissimo.shortener_service.exception.LinkNotFoundException;
import com.mrdanissimo.shortener_service.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkCacheService {

    private final LinkRepository linkRepository;

    @Cacheable(value = "originalUrls", key = "#shortCode")
    public CachedLink getLink(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));

        return new CachedLink(
                link.getOriginalUrl(),
                link.getExpiresAt()
        );
    }
}