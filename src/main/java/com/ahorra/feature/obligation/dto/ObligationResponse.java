package com.ahorra.feature.obligation.dto;

import com.ahorra.feature.obligation.enums.ObligationCadence;

import java.math.BigDecimal;

/** One fixed obligation as the client shows it. */
public record ObligationResponse(
        Long id,
        String name,
        BigDecimal amount,
        ObligationCadence cadence,
        boolean active
) {
}
