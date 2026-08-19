package com.mrdanissimo.analytics_service.event;

import java.time.LocalDateTime;

public record LinkClickedEvent(
        String shortCode,
        String originalUrl,
        LocalDateTime clickedAt,
        String userAgent,
        String correlationId
) {}
