package com.ahorra.common.exception;

import lombok.Getter;

/** The PIN supplied does not match. Carries how many tries are left before the PIN locks. */
@Getter
public class IncorrectPinException extends RuntimeException {

    private final int attemptsRemaining;

    public IncorrectPinException(String message, int attemptsRemaining) {
        super(message);
        this.attemptsRemaining = attemptsRemaining;
    }
}
