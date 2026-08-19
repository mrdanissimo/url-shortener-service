package com.mrdanissimo.shortener_service.dto;

public record LinkAnalyticsResponse(
        String shortCode,
        long totalClicks
) {}
