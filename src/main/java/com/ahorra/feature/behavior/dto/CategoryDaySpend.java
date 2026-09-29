package com.ahorra.feature.behavior.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Confirmed spend for one category on one day (null categoryId = uncategorized). */
public record CategoryDaySpend(LocalDate day, Long categoryId, BigDecimal amount) {
}
