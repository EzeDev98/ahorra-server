package com.ahorra.feature.capture.enums;

/**
 * Lifecycle of a pending (staged) entry. Only PENDING is written by the
 * capture flow; the confirm/edit endpoints move it on.
 */
public enum PendingStatus {
    PENDING,
    CONFIRMED,
    EDITED
}
