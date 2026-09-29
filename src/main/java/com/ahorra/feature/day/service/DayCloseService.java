package com.ahorra.feature.day.service;

import java.time.LocalDate;

public interface DayCloseService {

    /**
     * Idempotent close-check. Closes any suspended day that is now clear, then
     * evaluates every day not yet evaluated, up to and including {@code through}:
     * a day with pending entries is SUSPENDED, any other day (including one with
     * no entries at all) is CLOSED.
     */
    void catchUp(Long userId, LocalDate through);

    boolean isClosed(Long userId, LocalDate day);
}
