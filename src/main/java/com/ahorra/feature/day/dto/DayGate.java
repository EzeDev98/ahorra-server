package com.ahorra.feature.day.dto;

import com.ahorra.feature.day.enums.GateReason;

import java.time.LocalDate;

/** Whether the user must clear pending entries before continuing, and which day to clear. */
public record DayGate(boolean gated, GateReason reason, LocalDate day, int otherDaysRemaining) {

    public static DayGate open() {
        return new DayGate(false, GateReason.NONE, null, 0);
    }
}
