package com.mrdanissimo.analytics_service.dto;

import java.time.LocalDateTime;

public record FailedClickEventResponse(
        Long id,
        String shortCode,
        String originalUrl,
        LocalDateTime clickedAt,
        String userAgent,
        String correlationId
) {
}