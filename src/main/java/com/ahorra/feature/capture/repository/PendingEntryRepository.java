package com.ahorra.feature.capture.repository;

import com.ahorra.feature.capture.model.PendingEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingEntryRepository extends JpaRepository<PendingEntry, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<PendingEntry> findByRawEntryId(Long rawEntryId);

    @EntityGraph(attributePaths = "items")
    Optional<PendingEntry> findByIdAndUserId(Long id, Long userId);
}
