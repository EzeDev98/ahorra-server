package com.ahorra.feature.pin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Where the user's PIN stands. {@code retryAfterSeconds} is 0 unless the PIN is locked. */
@Getter
@AllArgsConstructor
public class PinStatusResponse {
    private final boolean pinSet;
    private final boolean locked;
    private final long retryAfterSeconds;
}
