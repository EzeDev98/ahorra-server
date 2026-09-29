package com.ahorra.feature.day.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Confirmed spend for one closed day. A day with nothing logged is a genuine 0.00. */
public record DaySpend(LocalDate day, BigDecimal spend) {
}
