package com.ahorra.feature.report.service.impl;

import com.ahorra.common.money.MoneyUtils;

import java.math.BigDecimal;

/** Shared number formatting for report text, so amounts read the same everywhere. */
public final class ReportFormat {

    private static final String CURRENCY = "₦";

    private ReportFormat() {
    }

    public static String money(BigDecimal amount) {
        return MoneyUtils.formatCurrency(MoneyUtils.abs(amount), CURRENCY);
    }

    public static String percent(BigDecimal percent) {
        return percent.stripTrailingZeros().toPlainString() + "%";
    }
}
