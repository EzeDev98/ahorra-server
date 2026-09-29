package com.ahorra.feature.income.service;

import com.ahorra.feature.income.dto.IncomeRequest;
import com.ahorra.feature.income.dto.IncomeResponse;

import java.util.List;

public interface IncomeService {

    /** The current income record. Throws EntityNotFoundException if none has been set. */
    IncomeResponse getCurrent(Long userId);

    /** Full history, newest first. */
    List<IncomeResponse> getHistory(Long userId);

    /**
     * Sets a new income. Closes the current record and starts a new one today,
     * or corrects the current record in place if it was created today.
     */
    IncomeResponse setIncome(Long userId, IncomeRequest request);
}
