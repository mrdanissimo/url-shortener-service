package com.mrdanissimo.analytics_service.repository;

import com.mrdanissimo.analytics_service.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    long countByShortCode(String shortCode);

    boolean existsByCorrelationId(String correlationId);
}
