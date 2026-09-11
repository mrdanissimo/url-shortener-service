package com.mrdanissimo.shortener_service.dto;

import java.time.LocalDateTime;

public record CachedLink(
        String originalUrl,
        LocalDateTime expiresAt
) {
}