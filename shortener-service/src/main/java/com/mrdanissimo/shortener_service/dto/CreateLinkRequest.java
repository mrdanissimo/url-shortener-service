package com.mrdanissimo.shortener_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLinkRequest {

    @NotBlank(message = "Требуется указаться URL")
    @URL(message = "Некорректный формат URL")
    private String originalUrl;

    private LocalDateTime expiresAt;
}
