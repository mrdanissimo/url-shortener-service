package com.mrdanissimo.shortener_service.service;

import com.mrdanissimo.shortener_service.dto.CreateLinkRequest;
import com.mrdanissimo.shortener_service.dto.LinkResponse;
import com.mrdanissimo.shortener_service.entity.Link;
import com.mrdanissimo.shortener_service.exception.LinkExpiredException;
import com.mrdanissimo.shortener_service.exception.LinkNotFoundException;
import com.mrdanissimo.shortener_service.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LinkService {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrsABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();
    private final LinkRepository linkRepository;

    // Генерация строки
    private String generateUniqueShortCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                int randomIndex = random.nextInt(ALPHABET.length());
                sb.append(ALPHABET.charAt(randomIndex));
            }
            code = sb.toString();
        } while (linkRepository.existsByShortCode(code));
        return code;
    }

    public LinkResponse createLink(CreateLinkRequest request) {
        String shortCode = generateUniqueShortCode();

        Link link = new Link();
        link.setOriginalUrl(request.getOriginalUrl());
        link.setShortCode(shortCode);
        link.setClicks(0L);
        link.setCreatedAt(LocalDateTime.now());

        Link savedLink = linkRepository.save(link);
        return mapToResponse(savedLink);
    }

    private LinkResponse mapToResponse(Link link) {
        return new LinkResponse(
                link.getId(),
                link.getOriginalUrl(),
                link.getShortCode(),
                link.getClicks(),
                link.getCreatedAt(),
                link.getExpiresAt()
        );
    }

    public LinkResponse getStats(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));

        return mapToResponse(link);
    }


    @Transactional
    public String redirect(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new LinkExpiredException("Срок действия ссылки истек");
        }

        link.setClicks(link.getClicks() + 1);
        linkRepository.save(link);

        return link.getOriginalUrl();
    }
}
