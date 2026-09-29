package com.ahorra.feature.day.event;

import java.time.LocalDate;

/**
 * Published when a day closes. retroactive is true when the day had been
 * suspended and was closed later, once the user cleared its backlog.
 * Listeners should use @TransactionalEventListener(AFTER_COMMIT).
 */
public record DayClosedEvent(Long userId, LocalDate day, boolean retroactive) {
}
