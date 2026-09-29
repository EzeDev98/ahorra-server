package com.ahorra.feature.behavior.calc;

import com.ahorra.feature.behavior.enums.TrendDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Layer 3: trend over up to 8 weekly windows. The input is each week's spend as a
 * percentage of that week's income baseline, oldest first. A least-squares line
 * gives the change in percentage points per week.
 */
@Component
public class TrendAnalyzer {

    public static final int MIN_WEEKS = 6;

    /** Percentage points per week that count as a real move. */
    static final double SLOPE_THRESHOLD = 0.5;

    public record TrendResult(TrendDirection direction,
                              BigDecimal slopePerWeek,
                              BigDecimal latestRatioPercent,
                              int weeksUsed) {
    }

    public Optional<TrendResult> analyze(List<Double> weeklyRatioPercent) {
        int weekCount = weeklyRatioPercent.size();
        if (weekCount < MIN_WEEKS) {
            return Optional.empty();
        }

        double meanWeekIndex = (weekCount - 1) / 2.0;
        double meanRatioPercent = weeklyRatioPercent.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        double sumOfCrossDeviations = 0;
        double sumOfSquaredWeekDeviations = 0;
        for (int weekIndex = 0; weekIndex < weekCount; weekIndex++) {
            double weekIndexDeviation = weekIndex - meanWeekIndex;
            double ratioDeviation = weeklyRatioPercent.get(weekIndex) - meanRatioPercent;
            sumOfCrossDeviations += weekIndexDeviation * ratioDeviation;
            sumOfSquaredWeekDeviations += weekIndexDeviation * weekIndexDeviation;
        }
        double slopePerWeek = sumOfSquaredWeekDeviations == 0 ? 0 : sumOfCrossDeviations / sumOfSquaredWeekDeviations;

        TrendDirection direction = slopePerWeek > SLOPE_THRESHOLD ? TrendDirection.WORSENING
                : slopePerWeek < -SLOPE_THRESHOLD ? TrendDirection.IMPROVING
                : TrendDirection.STABLE;

        double latestRatioPercent = weeklyRatioPercent.get(weekCount - 1);

        return Optional.of(new TrendResult(
                direction,
                BaselineCalculator.money(slopePerWeek),
                BaselineCalculator.money(latestRatioPercent),
                weekCount));
    }
}