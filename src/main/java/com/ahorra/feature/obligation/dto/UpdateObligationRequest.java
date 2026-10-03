package com.ahorra.feature.obligation.dto;

import com.ahorra.feature.obligation.enums.ObligationCadence;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** The body of PUT /obligations/{id}: the whole obligation again, including whether it is active. */
public record UpdateObligationRequest(
        @NotBlank(message = "name is required")
        @Size(max = 60, message = "name is too long (max 60 characters)")
        String name,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be more than zero")
        @Digits(integer = 13, fraction = 2, message = "amount allows up to 2 decimal places")
        BigDecimal amount,
        @NotNull(message = "cadence is required")
        ObligationCadence cadence,
        @NotNull(message = "active is required")
        Boolean active
) {
}
