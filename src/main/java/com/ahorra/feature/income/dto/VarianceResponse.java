package com.ahorra.feature.income.dto;

import com.ahorra.feature.accounting.enums.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * variance = spend - baseline, where baseline is the income range's lower bound.
 * Negative means under the baseline; positive means over it. Numbers only: the
 * wording of any verdict belongs to the report layer.
 */
public record VarianceResponse(
        LocalDate asOf,
        PeriodType periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        int daysElapsed,
        int daysInPeriod,
        BigDecimal incomeMin,
        BigDecimal incomeMax,
        BigDecimal baseline,
        BigDecimal spend,
        BigDecimal variance,
        List<CategoryBreakdown> categories
) {
}
