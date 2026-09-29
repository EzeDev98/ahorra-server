package com.ahorra.feature.report.service.impl;

import com.ahorra.common.money.MoneyUtils;
import com.ahorra.feature.report.dto.CategoryLine;
import com.ahorra.feature.report.dto.ReportFacts;
import com.ahorra.feature.report.enums.ReportVerdict;
import com.ahorra.feature.report.service.ReportFormat;
import org.springframework.stereotype.Component;

/**
 * The paragraph used when the model is unavailable or its output fails the
 * number check. Purely descriptive: it states the figures and lets the reader
 * draw their own conclusion.
 */
@Component
public class TemplateNarrative {

    public String write(ReportFacts f) {
        StringBuilder sb = new StringBuilder();

        if (MoneyUtils.isPositive(f.daySpend())) {
            sb.append("Confirmed spending for the day was ").append(ReportFormat.money(f.daySpend()))
                    .append(" across ").append(f.entryCount()).append(f.entryCount() == 1 ? " entry" : " entries");
            if (!f.dayCategories().isEmpty()) {
                CategoryLine top = f.dayCategories().get(0);
                sb.append(", with ").append(top.name()).append(" the largest at ")
                        .append(ReportFormat.money(top.amount())).append(" (")
                        .append(ReportFormat.percent(top.sharePercent())).append(" of the day)");
            }
            sb.append(". ");
        } else {
            sb.append("The day's ").append(f.entryCount()).append(f.entryCount() == 1 ? " entry was" : " entries were")
                    .append(" not spending, so no spending was recorded. ");
        }

        if (f.verdict() != ReportVerdict.NO_INCOME_SET) {
            boolean above = MoneyUtils.isPositive(f.variance());
            sb.append("So far this ").append(periodLabel(f)).append(" you have spent ")
                    .append(ReportFormat.money(f.periodSpend())).append(" against an income baseline of ")
                    .append(ReportFormat.money(f.baseline())).append(" (range ")
                    .append(ReportFormat.money(f.baseline())).append(" to ")
                    .append(ReportFormat.money(f.incomeMax())).append("), which is ")
                    .append(ReportFormat.money(f.variance())).append(above ? " above" : " below")
                    .append(" the baseline. ");
        }

        if (f.comparisonAvailable() && f.rollingAvg() != null) {
            sb.append("Your usual daily spend over the last 30 days is ")
                    .append(ReportFormat.money(f.rollingAvg())).append(". ");
        }
        if (f.goodDay()) {
            sb.append("Spending is running below the pace your income baseline implies. ");
        }

        String text = sb.toString().trim();
        return text.length() > 900 ? text.substring(0, 900) : text;
    }

    private String periodLabel(ReportFacts f) {
        return switch (f.periodType()) {
            case DAILY -> "day";
            case WEEKLY -> "week";
            case MONTHLY -> "month";
            case CUSTOM -> "period";
        };
    }
}
