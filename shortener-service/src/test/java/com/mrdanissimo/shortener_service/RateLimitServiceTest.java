package com.mrdanissimo.shortener_service;

import com.mrdanissimo.shortener_service.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitService rateLimitService;

    private final String clientIp = "192.168.1.1";
    private final String redisKey = "rate_limit:" + clientIp;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Первый запрос с IP - не заблокирован")
    void firstRequest_ShouldNotBeLimited() {
        when(valueOperations.increment(redisKey)).thenReturn(1L);

        boolean isLimited = rateLimitService.isRateLimited(clientIp);

        assertFalse(isLimited);
        verify(redisTemplate).expire(eq(redisKey), any(Duration.class));
    }

    @Test
    @DisplayName("11-й запрос с того же IP - заблокирован (возвращает true)")
    void eleventhRequest_ShouldBeLimited() {
        when(valueOperations.increment(redisKey)).thenReturn(11L);

        boolean isLimited = rateLimitService.isRateLimited(clientIp);

        assertTrue(isLimited);
    }

    @Test
    @DisplayName("Первый запрос устанавливает TTL")
    void firstRequest_ShouldSetTtl() {
        when(valueOperations.increment(redisKey)).thenReturn(1L);

        rateLimitService.isRateLimited(clientIp);

        verify(redisTemplate, times(1)).expire(eq(redisKey), any(Duration.class));
    }

    @Test
    @DisplayName("Последующие запросы не сбрасывают TTL")
    void subsequentRequests_ShouldNotResetTtl() {
        when(valueOperations.increment(redisKey)).thenReturn(2L);

        rateLimitService.isRateLimited(clientIp);

        verify(redisTemplate, never()).expire(eq(redisKey), any(Duration.class));
    }
}
