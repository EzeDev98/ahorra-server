package com.ahorra.feature.behavior;

/** Sentinel category ids so every stored row has a non-null category key. */
public final class BehaviorScope {

    /** All expense spend combined. */
    public static final long TOTAL = 0L;

    /** Spend on entries that carry no category tag. */
    public static final long UNCATEGORIZED = -1L;

    private BehaviorScope() {
    }
}
