package com.ahorra.feature.income.service;

import com.ahorra.feature.income.dto.VarianceResponse;

import java.time.LocalDate;

public interface VarianceService {

    /**
     * Spend so far in the period containing {@code asOf} (today when null),
     * measured against the lower-bound baseline of the income record that was
     * active on that date. Throws EntityNotFoundException if no income was set then.
     */
    VarianceResponse variance(Long userId, LocalDate asOf);
}
