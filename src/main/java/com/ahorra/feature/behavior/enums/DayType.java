package com.ahorra.feature.behavior.enums;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum DayType {
    ALL,
    WEEKDAY,
    WEEKEND;

    /** Whether a calendar day belongs to this segment. ALL matches everything. */
    public boolean matches(LocalDate day) {
        DayOfWeek d = day.getDayOfWeek();
        boolean weekend = d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
        return switch (this) {
            case ALL -> true;
            case WEEKEND -> weekend;
            case WEEKDAY -> !weekend;
        };
    }
}
