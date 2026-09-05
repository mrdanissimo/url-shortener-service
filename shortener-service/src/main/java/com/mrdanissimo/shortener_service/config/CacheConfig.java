package com.mrdanissimo.shortener_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mrdanissimo.shortener_service.dto.LinkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig implements CachingConfigurer {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        Jackson2JsonRedisSerializer<LinkResponse> linkResponseSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, LinkResponse.class);

        RedisCacheConfiguration originalUrlsConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(1))
                        .disableCachingNullValues()
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        stringSerializer
                                )
                        )
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        stringSerializer
                                )
                        );

        RedisCacheConfiguration linkInfoConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(1))
                        .disableCachingNullValues()
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        stringSerializer
                                )
                        )
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        linkResponseSerializer
                                )
                        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(originalUrlsConfig)
                .withInitialCacheConfigurations(
                        java.util.Map.of(
                                "originalUrls", originalUrlsConfig,
                                "linkInfo", linkInfoConfig
                        )
                )
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {

            @Override
            public void handleCacheGetError(
                    RuntimeException exception,
                    Cache cache,
                    Object key
            ) {
                log.error(
                        "Redis GET error for key {}: {}. Falling back to DB.",
                        key,
                        exception.getMessage()
                );
            }

            @Override
            public void handleCachePutError(
                    RuntimeException exception,
                    Cache cache,
                    Object key,
                    Object value
            ) {
                log.error(
                        "Redis PUT error for key {}: {}",
                        key,
                        exception.getMessage()
                );
            }

            @Override
            public void handleCacheEvictError(
                    RuntimeException exception,
                    Cache cache,
                    Object key
            ) {
                log.error(
                        "Redis EVICT error for key {}: {}",
                        key,
                        exception.getMessage()
                );
            }

            @Override
            public void handleCacheClearError(
                    RuntimeException exception,
                    Cache cache
            ) {
                log.error(
                        "Redis CLEAR error: {}",
                        exception.getMessage()
                );
            }
        };
    }
}