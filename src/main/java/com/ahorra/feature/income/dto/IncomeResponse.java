package com.ahorra.feature.income.dto;

import com.ahorra.feature.accounting.enums.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One income record. effectiveTo is exclusive and null for the current record. */
public record IncomeResponse(
        Long id,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        PeriodType periodType,
        Integer periodLength,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
