package com.mrdanissimo.shortener_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrdanissimo.shortener_service.entity.OutboxEvent;
import com.mrdanissimo.shortener_service.repository.OutboxEventRepository;
import com.mrdanissimo.shortener_service.service.OutboxRelay;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, JsonNode> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private OutboxRelay outboxRelay;

    @Test
    void processOutbox_whenKafkaAvailable_marksSent() throws Exception {

        // arrange
        OutboxEvent event = new OutboxEvent();

        event.setId(UUID.randomUUID());
        event.setAggregateType("LINK");
        event.setAggregateId("L7W4g0");
        event.setEventType("LINK_CLICKED");
        event.setPayload("{\"shortCode\":\"L7W4g0\"}");
        event.setStatus("PENDING");

        JsonNode payload = mock(JsonNode.class);

        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(List.of(event));

        when(objectMapper.readTree(event.getPayload()))
                .thenReturn(payload);

        when(kafkaTemplate.send(
                eq("link-clicks"),
                eq("L7W4g0"),
                eq(payload)
        )).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        // act
        outboxRelay.processOutbox();

        // assert
        assertEquals("SENT", event.getStatus());

        verify(outboxRepository).save(event);
    }

    @Test
    void processOutbox_whenKafkaFails_keepsPending() throws Exception {

        // arrange
        OutboxEvent event = new OutboxEvent();

        event.setId(UUID.randomUUID());
        event.setAggregateType("LINK");
        event.setAggregateId("L7W4g0");
        event.setEventType("LINK_CLICKED");
        event.setPayload("{\"shortCode\":\"L7W4g0\"}");
        event.setStatus("PENDING");

        JsonNode payload = mock(JsonNode.class);

        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(List.of(event));

        when(objectMapper.readTree(event.getPayload()))
                .thenReturn(payload);

        CompletableFuture<?> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(
                new RuntimeException("Kafka unavailable")
        );

        when(kafkaTemplate.send(
                eq("link-clicks"),
                eq("L7W4g0"),
                eq(payload)
        )).thenReturn(
                (CompletableFuture) failedFuture
        );

        // act
        outboxRelay.processOutbox();

        // assert
        assertEquals("PENDING", event.getStatus());

        verify(outboxRepository, never()).save(event);
    }
}