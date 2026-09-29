package com.ahorra.feature.report.enums;

public enum ReportKind {
    /** The day closed with nothing logged. Says so, and never asserts a spend total. */
    NO_ENTRIES,
    /** A full daily report for a day that had entries. */
    STANDARD
}
