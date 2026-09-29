package com.ahorra.feature.capture.repository;

import com.ahorra.feature.capture.model.RawEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RawEntryRepository extends JpaRepository<RawEntry, Long> {
    Optional<RawEntry> findByIdAndUserId(Long id, Long userId);
    Optional<RawEntry> findByUserIdAndClientRequestId(Long userId, String clientRequestId);
}
