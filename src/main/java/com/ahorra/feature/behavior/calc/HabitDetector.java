package com.ahorra.feature.behavior.calc;

import com.ahorra.feature.behavior.enums.TrendDirection;
import com.ahorra.feature.day.dto.DaySpend;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Layer 2: habit detection. A habit is a recurring, low-variance pattern in one
 * category, not a single-day spike. Input is that category's zero-filled series
 * of closed days (oldest first), normally the last 56 days.
 */
@Component
public class HabitDetector {

    static final int MIN_DAYS = 21;
    static final double MIN_PER_WEEK = 2.0;
    static final double MAX_VARIATION = 0.5;
    static final double MIN_WEEK_COVERAGE = 0.75;
    static final double TREND_CHANGE = 0.15;

    public record HabitSignal(BigDecimal frequencyPerWeek,
                              BigDecimal avgAmount,
                              LocalDate firstObserved,
                              LocalDate lastObserved,
                              TrendDirection trend,
                              BigDecimal confidence) {
    }

    public Optional<HabitSignal> detect(List<DaySpend> series) {
        int n = series.size();
        if (n < MIN_DAYS) {
            return Optional.empty();
        }
        List<DaySpend> active = series.stream().filter(d -> d.spend().signum() > 0).toList();
        if (active.size() < 2) {
            return Optional.empty();
        }

        // Frequency: how often it happens.
        double frequency = active.size() / (n / 7.0);
        if (frequency < MIN_PER_WEEK) {
            return Optional.empty();
        }

        // Regularity of amount: low coefficient of variation across the active days.
        double mean = active.stream().mapToDouble(d -> d.spend().doubleValue()).average().orElse(0);
        if (mean <= 0) {
            return Optional.empty();
        }
        double sumSquares = 0;
        for (DaySpend d : active) {
            double diff = d.spend().doubleValue() - mean;
            sumSquares += diff * diff;
        }
        double variation = Math.sqrt(sumSquares / (active.size() - 1)) / mean;
        if (variation > MAX_VARIATION) {
            return Optional.empty();
        }

        // Regularity over time: present in most weeks, not one burst.
        int blocks = n / 7;
        List<DaySpend> recent = series.subList(n - blocks * 7, n);
        double[] weekly = new double[blocks];
        int weeksPresent = 0;
        for (int b = 0; b < blocks; b++) {
            boolean any = false;
            for (int i = 0; i < 7; i++) {
                BigDecimal spend = recent.get(b * 7 + i).spend();
                weekly[b] += spend.doubleValue();
                if (spend.signum() > 0) {
                    any = true;
                }
            }
            if (any) {
                weeksPresent++;
            }
        }
        double coverage = weeksPresent / (double) blocks;
        if (coverage < MIN_WEEK_COVERAGE) {
            return Optional.empty();
        }

        // Direction: compare weekly spend in the first half with the second half.
        int half = blocks / 2;
        double first = 0;
        double second = 0;
        for (int i = 0; i < half; i++) {
            first += weekly[i];
            second += weekly[blocks - 1 - i];
        }
        TrendDirection trend;
        if (first <= 0) {
            trend = second > 0 ? TrendDirection.WORSENING : TrendDirection.STABLE;
        } else {
            double change = (second - first) / first;
            trend = change > TREND_CHANGE ? TrendDirection.WORSENING
                    : change < -TREND_CHANGE ? TrendDirection.IMPROVING
                    : TrendDirection.STABLE;
        }

        double confidence = Math.min(n / (double) BaselineCalculator.FULL_CONFIDENCE_DAYS, 1.0) * coverage;

        return Optional.of(new HabitSignal(
                BigDecimal.valueOf(frequency).setScale(2, RoundingMode.HALF_UP),
                BaselineCalculator.money(mean),
                active.get(0).day(),
                active.get(active.size() - 1).day(),
                trend,
                BigDecimal.valueOf(confidence).setScale(3, RoundingMode.HALF_UP)));
    }
}
