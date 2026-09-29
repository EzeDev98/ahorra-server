package com.ahorra.feature.report.service;

import com.ahorra.feature.report.dto.DailyReportResponse;
import com.ahorra.feature.report.dto.DailyReportSummary;

import java.time.LocalDate;
import java.util.List;

public interface ReportQueryService {

    /** Full report for one day. Throws EntityNotFoundException (404) when none is stored. */
    DailyReportResponse forDay(Long userId, LocalDate day);

    /** The most recent stored report. Throws EntityNotFoundException (404) when there are none. */
    DailyReportResponse latest(Long userId);

    /**
     * Newest-first summaries. Defaults to the last 30 days ending today; the span is
     * capped at 92 days (the newest end of the range wins).
     */
    List<DailyReportSummary> history(Long userId, LocalDate from, LocalDate to);
}
