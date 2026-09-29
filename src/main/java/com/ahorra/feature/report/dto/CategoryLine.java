package com.ahorra.feature.report.dto;

import java.math.BigDecimal;

/** A category's spend. attributedExcess is null for day-level lines. */
public record CategoryLine(String name, BigDecimal amount, BigDecimal sharePercent, BigDecimal attributedExcess) {
}
