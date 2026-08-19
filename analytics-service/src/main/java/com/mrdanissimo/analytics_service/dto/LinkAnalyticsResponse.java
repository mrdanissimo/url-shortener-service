package com.mrdanissimo.analytics_service.dto;

public record LinkAnalyticsResponse(
        String shortCode,
        long totalClicks
) {}
