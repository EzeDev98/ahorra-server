package com.ahorra.feature.report.enums;

/** Spending relative to the income baseline's pace for the period. */
public enum ReportVerdict {
    OVER,
    ON_TRACK,
    UNDER,
    /** No income record existed for that day, so there was nothing to compare against. */
    NO_INCOME_SET
}
