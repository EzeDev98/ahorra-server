package com.ahorra.feature.income.dto;

import com.ahorra.feature.accounting.enums.PeriodType;

import java.math.BigDecimal;

/**
 * The user's stated income range for a period. periodLength (in days) is only
 * used, and required, when periodType is CUSTOM.
 */
public record IncomeRequest(
        BigDecimal minAmount,
        BigDecimal maxAmount,
        PeriodType periodType,
        Integer periodLength
) {
}
