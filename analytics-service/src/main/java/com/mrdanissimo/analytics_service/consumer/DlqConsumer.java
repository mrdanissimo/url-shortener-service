package com.mrdanissimo.analytics_service.consumer;

import com.mrdanissimo.analytics_service.entity.FailedClickEvent;
import com.mrdanissimo.analytics_service.event.LinkClickedEvent;
import com.mrdanissimo.analytics_service.repository.FailedClickEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DlqConsumer {

    private final FailedClickEventRepository repository;

    @KafkaListener(
            topics = "link-clicks-dead-letter",
            groupId = "analytics-dlq-group"
    )
    public void consumeDlq(LinkClickedEvent event) {

        FailedClickEvent failedEvent = FailedClickEvent.builder()
                .shortCode(event.shortCode())
                .originalUrl(event.originalUrl())
                .clickedAt(event.clickedAt())
                .userAgent(event.userAgent())
                .correlationId(event.correlationId())
                .build();

        repository.save(failedEvent);

        log.error(
                "[DLQ] Failed event saved: shortCode={}, correlationId={}",
                event.shortCode(),
                event.correlationId()
        );
    }
}