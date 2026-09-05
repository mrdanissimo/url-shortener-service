package com.mrdanissimo.analytics_service.controller;

import com.mrdanissimo.analytics_service.dto.FailedClickEventResponse;
import com.mrdanissimo.analytics_service.dto.LinkAnalyticsResponse;
import com.mrdanissimo.analytics_service.repository.ClickEventRepository;
import com.mrdanissimo.analytics_service.repository.FailedClickEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<Page<FailedClickEventResponse>> getFailedEvents(
            @PageableDefault(
                    size = 20,
                    sort = "clickedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        Page<FailedClickEventResponse> events =
                failedClickEventRepository.findAll(pageable)
                        .map(event -> new FailedClickEventResponse(
                                event.getId(),
                                event.getShortCode(),
                                event.getOriginalUrl(),
                                event.getClickedAt(),
                                event.getUserAgent(),
                                event.getCorrelationId()
                        ));

        return ResponseEntity.ok(events);
    }
}
