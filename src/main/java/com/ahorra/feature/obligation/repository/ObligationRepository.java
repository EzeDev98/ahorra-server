package com.ahorra.feature.obligation.repository;

import com.ahorra.feature.obligation.model.Obligation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObligationRepository extends JpaRepository<Obligation, Long> {
    List<Obligation> findAllByUserIdOrderByIdAsc(Long userId);
    Optional<Obligation> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
}
