package com.mrdanissimo.shortener_service.controller;

import com.mrdanissimo.shortener_service.client.AnalyticsClient;
import com.mrdanissimo.shortener_service.dto.CreateLinkRequest;
import com.mrdanissimo.shortener_service.dto.LinkAnalyticsResponse;
import com.mrdanissimo.shortener_service.dto.LinkResponse;
import com.mrdanissimo.shortener_service.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;
    private final AnalyticsClient analyticsClient;

    @PostMapping("/api/links")
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        LinkResponse response = linkService.createLink(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent
    ) {
        String originalUrl = linkService.redirect(shortCode, userAgent);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }


    @GetMapping("/api/links/{shortCode}/stats")
    public ResponseEntity<LinkResponse> getStats(@PathVariable String shortCode) {
        LinkResponse response = linkService.getStats(shortCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/links/{shortCode}")
    public ResponseEntity<LinkResponse> getLinkInfo(@PathVariable String shortCode) {
        LinkResponse response = linkService.getLinkInfo(shortCode);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/links/{shortCode}")
    public ResponseEntity<Void> deleteLink(@PathVariable String shortCode) {
        linkService.deleteLink(shortCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/links/{shortCode}/analytics")
    public ResponseEntity<LinkAnalyticsResponse> getDetailedAnalytics(@PathVariable String shortCode) {
        LinkAnalyticsResponse response = analyticsClient.getAnalytics(shortCode);
        return ResponseEntity.ok(response);
    }

}
