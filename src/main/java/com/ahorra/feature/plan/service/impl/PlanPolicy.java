package com.ahorra.feature.plan.service.impl;

import java.util.Set;

/** The fixed rules of the plan: the split, how long the starter phase lasts, and which categories count as Needs. */
final class PlanPolicy {

    static final int NEEDS_PERCENT = 50;
    static final int WANTS_PERCENT = 30;
    static final int SAVINGS_PERCENT = 20;

    /** Days from the start of the account during which only the generic split is shown. */
    static final int STARTER_DAYS = 30;

    /** The categories that count as Needs. Every other category, and uncategorized spend, counts as Wants. */
    static final Set<String> NEEDS_CATEGORIES = Set.of(
            "Food",
            "Transport",
            "Airtime and data",
            "Bills and utilities",
            "Rent",
            "Health",
            "Education"
    );

    private PlanPolicy() {
    }
}
