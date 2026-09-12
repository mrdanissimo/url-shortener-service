package com.mrdanissimo.analytics_service.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record LinkClickedEvent(
        UUID eventId,
        String shortCode,
        String originalUrl,
        LocalDateTime clickedAt,
        String userAgent,
        String correlationId
) {}