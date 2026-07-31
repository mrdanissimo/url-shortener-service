package com.mrdanissimo.shortener_service.repository;

import com.mrdanissimo.shortener_service.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    boolean existsByShortCode(String shortCode);

    Optional<Link> findByShortCode(String shortCode);
}
