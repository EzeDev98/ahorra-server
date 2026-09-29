package com.ahorra.feature.behavior.service;

import com.ahorra.feature.behavior.calc.TrendAnalyzer.TrendResult;

import java.time.LocalDate;
import java.util.Optional;

public interface BehaviorAnalysisService {

    /**
     * Recomputes the user's baselines and habits from closed days up to asOf and
     * upserts them. Runs in its own transaction so it is safe after a commit.
     */
    void refresh(Long userId, LocalDate asOf);

    /** 8-week spend-to-income trend. Empty when fewer than 6 complete weeks have an income baseline. */
    Optional<TrendResult> trend(Long userId, LocalDate asOf);
}
