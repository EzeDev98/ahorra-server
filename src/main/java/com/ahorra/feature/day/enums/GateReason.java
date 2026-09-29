package com.ahorra.feature.day.enums;

public enum GateReason {
    /** Not gated. */
    NONE,
    /** An earlier day still has unconfirmed entries and must be cleared first. */
    SUSPENDED_DAY,
    /** Today has unconfirmed entries and the evening cutoff has passed. */
    HARD_GATE_TODAY
}
