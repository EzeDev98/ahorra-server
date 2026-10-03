package com.ahorra.feature.plan.service;

import com.ahorra.feature.plan.dto.PlanResponse;

public interface PlanService {

    /**
     * The 50/30/20 plan for the income period that contains today. Throws EntityNotFoundException
     * if the user has not stated an income yet.
     */
    PlanResponse currentPlan(Long userId);
}
