package com.ahorra.feature.day.enums;

/** Outcome of a day's close-check. A day with no row has not been evaluated yet. */
public enum DayStatus {
    /** Closed at (or after) the day's end; its summary can fire. */
    CLOSED,
    /** Pending entries were still outstanding at close; stays suspended until cleared. */
    SUSPENDED
}
