package com.mrdanissimo.shortener_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrdanissimo.shortener_service.entity.OutboxEvent;
import com.mrdanissimo.shortener_service.event.LinkClickedEvent;
import com.mrdanissimo.shortener_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void saveLinkClickedEvent(LinkClickedEvent event) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();

            outboxEvent.setId(UUID.randomUUID());
            outboxEvent.setAggregateType("LINK");
            outboxEvent.setAggregateId(event.shortCode());
            outboxEvent.setEventType("LINK_CLICKED");
            outboxEvent.setPayload(
                    objectMapper.writeValueAsString(event)
            );
            outboxEvent.setStatus("PENDING");
            outboxEvent.setCreatedAt(LocalDateTime.now());

            outboxEventRepository.save(outboxEvent);

            log.info(
                    "Link click saved to outbox for shortCode: {}",
                    event.shortCode()
            );

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize LinkClickedEvent",
                    e
            );
        }
    }
}