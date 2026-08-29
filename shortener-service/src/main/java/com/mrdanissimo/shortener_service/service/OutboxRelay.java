package com.mrdanissimo.shortener_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrdanissimo.shortener_service.entity.OutboxEvent;
import com.mrdanissimo.shortener_service.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, JsonNode> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private final AtomicLong pendingCount = new AtomicLong();

    @PostConstruct
    public void initMetrics() {
        meterRegistry.gauge(
                "outbox.pending.count",
                pendingCount
        );
    }

    @Scheduled(fixedDelay = 5000)
    public void processOutbox() {

        List<OutboxEvent> events =
                outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : events) {
            try {
                JsonNode payload = objectMapper.readTree(event.getPayload());

                kafkaTemplate.send(
                        "link-clicks",
                        event.getAggregateId(),
                        payload
                ).get();

                event.setStatus("SENT");
                event.setSentAt(LocalDateTime.now());

                outboxEventRepository.save(event);

                log.info(
                        "Outbox event {} sent to Kafka",
                        event.getId()
                );

            } catch (Exception e) {
                log.error(
                        "Failed to send outbox event {}",
                        event.getId(),
                        e
                );
            }
        }

        long count = outboxEventRepository.countByStatus("PENDING");
        pendingCount.set(count);
    }
}