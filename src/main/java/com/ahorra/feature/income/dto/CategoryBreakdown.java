package com.ahorra.feature.income.dto;

import java.math.BigDecimal;

/**
 * One category's part of the period's spend. attributedExcess is this category's
 * proportional share of the overspend, and is 0.00 when the user is not over.
 */
public record CategoryBreakdown(
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        BigDecimal shareOfSpendPercent,
        BigDecimal attributedExcess
) {
}
