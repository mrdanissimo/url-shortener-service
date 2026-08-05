package com.mrdanissimo.shortener_service.controller;

import com.mrdanissimo.shortener_service.dto.CreateLinkRequest;
import com.mrdanissimo.shortener_service.dto.LinkResponse;
import com.mrdanissimo.shortener_service.service.LinkService;
import com.mrdanissimo.shortener_service.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @PostMapping("/api/links")
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        LinkResponse response = linkService.createLink(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        LinkResponse link = linkService.getLinkInfo(shortCode);

        linkService.incrementClicks(shortCode);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, link.getOriginalUrl())
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
}
