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
        int n = weeklyRatioPercent.size();
        if (n < MIN_WEEKS) {
            return Optional.empty();
        }
        double xMean = (n - 1) / 2.0;
        double yMean = weeklyRatioPercent.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double num = 0;
        double den = 0;
        for (int i = 0; i < n; i++) {
            double dx = i - xMean;
            num += dx * (weeklyRatioPercent.get(i) - yMean);
            den += dx * dx;
        }
        double slope = den == 0 ? 0 : num / den;

        TrendDirection direction = slope > SLOPE_THRESHOLD ? TrendDirection.WORSENING
                : slope < -SLOPE_THRESHOLD ? TrendDirection.IMPROVING
                : TrendDirection.STABLE;

        return Optional.of(new TrendResult(
                direction,
                BaselineCalculator.money(slope),
                BaselineCalculator.money(weeklyRatioPercent.get(n - 1)),
                n));
    }
}
