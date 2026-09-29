package com.ahorra.feature.behavior.dto;

import com.ahorra.feature.behavior.enums.TrendDirection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HabitView(
        String description,
        BigDecimal frequencyPerWeek,
        BigDecimal avgAmount,
        LocalDate firstObserved,
        LocalDate lastObserved,
        TrendDirection trendDirection,
        BigDecimal confidence
) {
}
