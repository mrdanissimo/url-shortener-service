package com.mrdanissimo.analytics_service.consumer;

import com.mrdanissimo.analytics_service.event.LinkClickedEvent;
import com.mrdanissimo.analytics_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClickEventConsumer {

    private final AnalyticsService analyticsService;

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = "-dead-letter"
    )
    @KafkaListener(topics = "link-clicks", groupId = "analytics-group")
    public void consume(LinkClickedEvent event) {

        try {
            MDC.put("correlationId", event.correlationId());

            log.info("Received LinkClickedEvent from Kafka: {}", event);

            analyticsService.saveClickEvent(event);

        } finally {
            MDC.clear();
        }
    }
}