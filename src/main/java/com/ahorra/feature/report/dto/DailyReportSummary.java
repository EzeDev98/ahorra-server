package com.ahorra.feature.report.dto;

import com.ahorra.feature.report.enums.ReportKind;
import com.ahorra.feature.report.enums.ReportVerdict;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Compact row for history lists. Fetch the day for the full report. */
public record DailyReportSummary(
        LocalDate date,
        ReportKind kind,
        Integer entryCount,
        BigDecimal daySpend,
        ReportVerdict verdict,
        String summary
) {
}
