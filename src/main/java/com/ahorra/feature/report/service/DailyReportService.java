package com.ahorra.feature.report.service;

import java.time.LocalDate;

public interface DailyReportService {

    /**
     * Generates and stores the report for a day, but only if that day is CLOSED and
     * has no report yet. Safe to call repeatedly.
     */
    void generateIfMissing(Long userId, LocalDate day);

    /** Backfills reports for the user's closed days that never got one (oldest first, capped). */
    void generateMissing(Long userId);
}
