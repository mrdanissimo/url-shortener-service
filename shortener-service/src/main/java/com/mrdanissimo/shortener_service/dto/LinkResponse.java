package com.mrdanissimo.shortener_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinkResponse implements Serializable {
    private Long id;
    private String originalUrl;
    private String shortCode;
    private Long clicks;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
