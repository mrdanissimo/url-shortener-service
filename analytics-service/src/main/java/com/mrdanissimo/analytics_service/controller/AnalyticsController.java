package com.mrdanissimo.analytics_service.controller;

import com.mrdanissimo.analytics_service.dto.LinkAnalyticsResponse;
import com.mrdanissimo.analytics_service.repository.ClickEventRepository;
import com.mrdanissimo.analytics_service.repository.FailedClickEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ClickEventRepository repository;
    private final FailedClickEventRepository failedClickEventRepository;

    @GetMapping("/{shortCode}")
    public ResponseEntity<LinkAnalyticsResponse> getAnalytics(@PathVariable String shortCode) {
        long totalClicks = repository.countByShortCode(shortCode);
        return ResponseEntity.ok(new LinkAnalyticsResponse(shortCode, totalClicks));
    }

    @GetMapping("/failed")
    public ResponseEntity<?> getFailedEvents() {
        return ResponseEntity.ok(failedClickEventRepository.findAll());
    }
}
