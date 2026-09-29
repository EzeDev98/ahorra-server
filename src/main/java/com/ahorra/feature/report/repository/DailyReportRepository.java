package com.ahorra.feature.report.repository;

import com.ahorra.feature.report.model.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {
    boolean existsByUserIdAndReportDate(Long userId, LocalDate reportDate);
    Optional<DailyReport> findByUserIdAndReportDate(Long userId, LocalDate reportDate);
}
