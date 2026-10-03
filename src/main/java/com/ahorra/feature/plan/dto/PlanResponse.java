package com.ahorra.feature.plan.dto;

import com.ahorra.feature.accounting.enums.PeriodType;
import com.ahorra.feature.plan.enums.PlanPhase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The spending plan for the income period that contains today. basis is the lower bound of the stated income.
 * planDay counts from the day the account started (1 on that day); the observed split appears once it passes
 * starterDays. Numbers only: the wording belongs to the client.
 */
public record PlanResponse(
        PlanPhase phase,
        int planDay,
        int starterDays,
        PeriodType periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        int daysElapsed,
        int daysInPeriod,
        BigDecimal basis,
        BigDecimal obligationsTotal,
        boolean obligationsExceedNeeds,
        List<PlanBucket> buckets
) {
}
