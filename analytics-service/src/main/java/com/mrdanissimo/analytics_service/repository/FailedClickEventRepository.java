package com.mrdanissimo.analytics_service.repository;

import com.mrdanissimo.analytics_service.entity.FailedClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedClickEventRepository extends JpaRepository<FailedClickEvent, Long> {
}