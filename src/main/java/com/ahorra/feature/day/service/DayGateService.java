package com.ahorra.feature.day.service;

import com.ahorra.feature.day.dto.DayGate;
import com.ahorra.feature.day.dto.DayStatusResponse;

public interface DayGateService {

    /** Works out, from the user's pending entries and the clock, whether they are gated. */
    DayGate evaluate(Long userId);

    /** Throws DayGatedException (HTTP 423) if the user is gated. */
    void assertNotGated(Long userId);

    /** Runs the close-check catch-up, then returns the gate state and the cards to clear. */
    DayStatusResponse status(Long userId);
}
