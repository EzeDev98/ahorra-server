package com.ahorra.common.exception;

/** Thrown when the user must clear pending entries before doing anything else. Maps to HTTP 423. */
public class DayGatedException extends RuntimeException {
    public DayGatedException(String message) {
        super(message);
    }
}
