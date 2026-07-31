package com.mrdanissimo.shortener_service;

import com.mrdanissimo.shortener_service.dto.CreateLinkRequest;
import com.mrdanissimo.shortener_service.dto.LinkResponse;
import com.mrdanissimo.shortener_service.entity.Link;
import com.mrdanissimo.shortener_service.exception.LinkNotFoundException;
import com.mrdanissimo.shortener_service.repository.LinkRepository;
import com.mrdanissimo.shortener_service.service.LinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @InjectMocks
    private LinkService linkService;

    private Link sampleLink;

    @BeforeEach
    void setUp() {
        sampleLink = new Link();
        sampleLink.setId(1L);
        sampleLink.setOriginalUrl("https://github.com");
        sampleLink.setShortCode("HLP0N0");
        sampleLink.setClicks(0L);
        sampleLink.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Создание ссылки сохраняет и возвращает response с правильными данными")
    void createLink_ShouldSaveAndReturnResponseDto() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setOriginalUrl("https://github.com");

        when(linkRepository.existsByShortCode(any())).thenReturn(false);
        when(linkRepository.save(any(Link.class))).thenAnswer(invocation -> {
            Link savedLink = invocation.getArgument(0);
            savedLink.setId(1L);
            return savedLink;
        });

        LinkResponse response = linkService.createLink(request);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalUrl()).isEqualTo("https://github.com");
        assertThat(response.getShortCode()).isNotNull().hasSize(6);
        assertThat(response.getClicks()).isEqualTo(0L);

        verify(linkRepository, times(1)).save(any(Link.class));
    }

    @Test
    @DisplayName("Поиск по существующему shortCode возвращает ссылку")
    void redirect_WhenCodeExists_ShouldReturnUrlAndIncrementClicks() {
        when(linkRepository.findByShortCode("HLP0N0")).thenReturn(Optional.of(sampleLink));

        String originalUrl = linkService.redirect("HLP0N0");

        assertThat(originalUrl).isEqualTo("https://github.com");
        assertThat(sampleLink.getClicks()).isEqualTo(1L);
        verify(linkRepository, times(1)).findByShortCode("HLP0N0");
        verify(linkRepository, times(1)).save(sampleLink);
    }

    @Test
    @DisplayName("Поиск по несуществующему shortCode бросает LinkNotFoundException")
    void redirect_WhenCodeDoesNotExist_ShouldThrowLinkNotFoundException() {
        when(linkRepository.findByShortCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkService.redirect("UNKNOWN"))
                .isInstanceOf(LinkNotFoundException.class);

        verify(linkRepository, times(1)).findByShortCode("UNKNOWN");
    }
}
