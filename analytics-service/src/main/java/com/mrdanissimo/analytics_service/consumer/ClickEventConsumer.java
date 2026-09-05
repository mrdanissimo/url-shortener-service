package com.mrdanissimo.analytics_service.consumer;

import com.mrdanissimo.analytics_service.event.LinkClickedEvent;
import com.mrdanissimo.analytics_service.service.AnalyticsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.retry.annotation.Backoff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClickEventConsumer {

    private final AnalyticsService analyticsService;
    private final Counter eventsProcessedTotal;

    public ClickEventConsumer(
            AnalyticsService analyticsService,
            MeterRegistry meterRegistry
    ) {
        this.analyticsService = analyticsService;

        this.eventsProcessedTotal = Counter.builder("analytics.events.processed.total")
                .description("Total number of successfully processed click events")
                .register(meterRegistry);
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    @KafkaListener(topics = "link-clicks", groupId = "analytics-group")
    public void consume(LinkClickedEvent event) {

        try {
            MDC.put("correlationId", event.correlationId());

            log.info("Received LinkClickedEvent from Kafka: {}", event);

            analyticsService.saveClickEvent(event);

            eventsProcessedTotal.increment();

        } finally {
            MDC.clear();
        }
    }
}