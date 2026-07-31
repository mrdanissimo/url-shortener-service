package com.mrdanissimo.shortener_service.controller;

import com.mrdanissimo.shortener_service.dto.CreateLinkRequest;
import com.mrdanissimo.shortener_service.dto.LinkResponse;
import com.mrdanissimo.shortener_service.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @PostMapping
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        LinkResponse response = linkService.createLink(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = linkService.redirect(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }

    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<LinkResponse> getStats(@PathVariable String shortCode) {
        LinkResponse response = linkService.getStats(shortCode);
        return ResponseEntity.ok(response);
    }

}
