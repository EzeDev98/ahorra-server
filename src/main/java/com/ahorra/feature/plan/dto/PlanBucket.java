package com.ahorra.feature.plan.dto;

import java.math.BigDecimal;

/**
 * One share of the plan for the current income period.
 * key is NEEDS, WANTS or SAVINGS. committed is what is already spoken for inside the target (fixed obligations,
 * Needs only). room is target minus committed and can be negative. observed is what confirmed entries show so far,
 * and is null while the plan is still in its starter phase.
 */
public record PlanBucket(
        String key,
        int sharePercent,
        BigDecimal target,
        BigDecimal committed,
        BigDecimal room,
        BigDecimal observed
) {
}
