package com.ahorra.feature.day.repository;

import com.ahorra.feature.day.enums.DayStatus;
import com.ahorra.feature.day.model.DayState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DayStateRepository extends JpaRepository<DayState, Long> {
    Optional<DayState> findTopByUserIdOrderByDayDateDesc(Long userId);
    List<DayState> findAllByUserIdAndStatus(Long userId, DayStatus status);
    boolean existsByUserIdAndDayDateAndStatus(Long userId, LocalDate dayDate, DayStatus status);
    List<DayState> findAllByUserIdAndStatusAndDayDateBetweenOrderByDayDateAsc(
            Long userId, DayStatus status, LocalDate from, LocalDate to);

    /** Days with the given status that have no stored report yet, oldest first. */
    @Query("""
            select s.dayDate from DayState s
            where s.userId = :userId and s.status = :status
              and not exists (select 1 from DailyReport r
                              where r.userId = s.userId and r.reportDate = s.dayDate)
            order by s.dayDate asc
            """)
    List<LocalDate> findDaysWithoutReport(@Param("userId") Long userId, @Param("status") DayStatus status);
}
