package com.ahorra.feature.day.dto;

import com.ahorra.feature.capture.dto.PendingEntryResponse;
import com.ahorra.feature.day.enums.GateReason;

import java.time.LocalDate;
import java.util.List;

/**
 * What the client needs on every app open. When gated, {@code pending} holds only
 * the cards for {@code day}; when not gated it holds all outstanding cards
 * (the soft nudge).
 */
public record DayStatusResponse(
        boolean gated,
        GateReason reason,
        LocalDate day,
        int otherDaysRemaining,
        String hardGateTime,
        List<PendingEntryResponse> pending
) {
}
