package com.mrdanissimo.shortener_service.repository;

import com.mrdanissimo.shortener_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(String status);

    long countByStatus(String status);

    long deleteByStatusAndSentAtBefore(String status, LocalDateTime dateTime);
}