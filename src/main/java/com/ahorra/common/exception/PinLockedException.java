package com.ahorra.common.exception;

import lombok.Getter;

/** Too many wrong PIN attempts. The PIN cannot be checked until the lock ends. */
@Getter
public class PinLockedException extends RuntimeException {

    private final long retryAfterSeconds;

    public PinLockedException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
