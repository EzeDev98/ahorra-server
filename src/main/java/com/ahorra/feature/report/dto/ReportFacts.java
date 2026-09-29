package com.ahorra.feature.report.dto;

import com.ahorra.feature.accounting.enums.PeriodType;
import com.ahorra.feature.report.enums.ReportVerdict;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Every number in a report, computed deterministically in the backend. The
 * narrative is written from these and may not contain anything else.
 * The income-related fields are null when no income was set for the day.
 */
public record ReportFacts(
        LocalDate day,
        int entryCount,
        BigDecimal daySpend,
        PeriodType periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        int daysElapsed,
        int daysInPeriod,
        BigDecimal baseline,
        BigDecimal incomeMax,
        BigDecimal periodSpend,
        BigDecimal variance,
        BigDecimal rollingAvg,
        int daysObserved,
        boolean comparisonAvailable,
        ReportVerdict verdict,
        String insight,
        boolean goodDay,
        List<CategoryLine> dayCategories,
        List<CategoryLine> periodCategories
) {

    /**
     * The set of numbers a narrative is allowed to contain, normalised (no commas,
     * no trailing zeros), so that anything else in it can be rejected.
     */
    public Set<String> allowedNumbers() {
        Set<String> allowed = new HashSet<>(Set.of("0", "100"));
        List<BigDecimal> values = new ArrayList<>();
        values.add(daySpend);
        values.add(baseline);
        values.add(incomeMax);
        values.add(periodSpend);
        values.add(variance);
        values.add(rollingAvg);
        for (CategoryLine line : dayCategories) {
            values.add(line.amount());
            values.add(line.sharePercent());
            values.add(line.attributedExcess());
        }
        for (CategoryLine line : periodCategories) {
            values.add(line.amount());
            values.add(line.sharePercent());
            values.add(line.attributedExcess());
        }
        for (BigDecimal value : values) {
            if (value != null) {
                allowed.add(value.abs().stripTrailingZeros().toPlainString());
            }
        }
        for (int number : new int[]{entryCount, daysElapsed, daysInPeriod, daysObserved,
                dayCategories.size(), periodCategories.size(),
                day.getDayOfMonth(), day.getMonthValue(), day.getYear()}) {
            allowed.add(Integer.toString(number));
        }
        return allowed;
    }
}
