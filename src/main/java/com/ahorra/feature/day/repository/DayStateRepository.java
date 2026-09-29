package com.ahorra.feature.day.repository;

import com.ahorra.feature.day.enums.DayStatus;
import com.ahorra.feature.day.model.DayState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DayStateRepository extends JpaRepository<DayState, Long> {
    Optional<DayState> findTopByUserIdOrderByDayDateDesc(Long userId);
    List<DayState> findAllByUserIdAndStatus(Long userId, DayStatus status);
    boolean existsByUserIdAndDayDateAndStatus(Long userId, LocalDate dayDate, DayStatus status);
    List<DayState> findAllByUserIdAndStatusAndDayDateBetweenOrderByDayDateAsc(
            Long userId, DayStatus status, LocalDate from, LocalDate to);
}
