package com.ahorra.feature.capture.enums;

/**
 * Lifecycle of a pending (staged) entry. Only PENDING is written by the
 * capture flow; the confirm/edit endpoints move it on. DISCARDED is a soft
 * removal: the row and its raw input are kept for audit, and it can be restored.
 */
public enum PendingStatus {
    PENDING,
    CONFIRMED,
    EDITED,
    DISCARDED
}
