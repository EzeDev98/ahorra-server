package com.ahorra.feature.behavior.dto;

import com.ahorra.feature.behavior.enums.TrendDirection;

import java.math.BigDecimal;

/**
 * Spend as a percentage of the income baseline, over up to 8 weekly windows.
 * slopePerWeek is in percentage points per week.
 */
public record TrendView(
        TrendDirection direction,
        BigDecimal slopePerWeek,
        BigDecimal latestRatioPercent,
        int weeksUsed,
        boolean livingAboveMeans
) {
}
