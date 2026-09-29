package com.ahorra.feature.report.dto;

import com.ahorra.feature.accounting.enums.PeriodType;
import com.ahorra.feature.report.enums.NarrativeSource;
import com.ahorra.feature.report.enums.ReportKind;
import com.ahorra.feature.report.enums.ReportVerdict;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** One stored report in full. Fields that do not apply to a NO_ENTRIES report are null. */
public record DailyReportResponse(
        LocalDate date,
        ReportKind kind,
        String summary,
        PeriodType periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        Integer entryCount,
        BigDecimal daySpend,
        BigDecimal baseline,
        BigDecimal incomeMax,
        BigDecimal periodSpend,
        BigDecimal variance,
        BigDecimal rollingAvgDailySpend,
        Integer daysObserved,
        ReportVerdict verdict,
        String insight,
        NarrativeSource narrativeSource,
        List<CategoryLine> dayCategories,
        List<CategoryLine> periodCategories
) {
}
