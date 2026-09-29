package com.ahorra.feature.behavior.enums;

public enum BehaviorMode {
    /** Fewer than 30 closed days: data is shown, no behavioral judgments are made. */
    OBSERVATION,
    /** 30 or more closed days: habits, trend and unusual-day signals are on. */
    JUDGMENT
}
