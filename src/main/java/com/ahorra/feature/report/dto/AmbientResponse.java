package com.ahorra.feature.report.dto;

import com.ahorra.feature.report.enums.ReportVerdict;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The running total shown while the day is still open.
 *
 * provisionalSpend = confirmedSpend + pendingSpend. It is for display only.
 * The verdict, periodVariance and periodBaseline use confirmed spend alone.
 */
public record AmbientResponse(
        LocalDate date,
        BigDecimal confirmedSpend,
        BigDecimal pendingSpend,
        int pendingCount,
        BigDecimal provisionalSpend,
        boolean hasUnconfirmed,
        BigDecimal usualDailySpend,
        int daysObserved,
        boolean comparisonAvailable,
        ReportVerdict verdict,
        BigDecimal periodVariance,
        BigDecimal periodBaseline
) {
}
