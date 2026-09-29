package com.ahorra.feature.behavior.dto;

import com.ahorra.feature.behavior.enums.BehaviorMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * In OBSERVATION mode habits is empty and trend, todayZScore and todayUnusual
 * are null. Baselines are always included.
 */
public record BehaviorOverview(
        LocalDate asOf,
        BehaviorMode mode,
        int daysObserved,
        int daysUntilJudgment,
        BigDecimal confidence,
        List<BaselineView> baselines,
        List<HabitView> habits,
        TrendView trend,
        BigDecimal todayZScore,
        Boolean todayUnusual
) {
}
