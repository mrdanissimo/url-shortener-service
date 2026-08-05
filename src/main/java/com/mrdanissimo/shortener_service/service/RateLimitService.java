package com.mrdanissimo.shortener_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS = 10;
    private static final int WINDOW_SECONDS = 60;

    public boolean isRateLimited(String ip) {
        String key = "rate_limit:" + ip;
        try {
            Long requests = redisTemplate.opsForValue().increment(key);
            if (requests != null && requests == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
            }
            return requests != null && requests > MAX_REQUESTS;
        } catch (Exception e) {
            log.error("Redis is down. Rate limiting bypassed for IP: {}", ip, e);
            return false;
        }
    }

}
