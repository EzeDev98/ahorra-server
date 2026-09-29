package com.ahorra.feature.report.listener;

import com.ahorra.feature.day.event.DayClosedEvent;
import com.ahorra.feature.report.service.AmbientAggregateCache;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Clears the cached usual-spend figure once a day-close has committed. */
@Component
public class AmbientCacheEvictListener {

    private final AmbientAggregateCache aggregateCache;

    public AmbientCacheEvictListener(AmbientAggregateCache aggregateCache) {
        this.aggregateCache = aggregateCache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDayClosed(DayClosedEvent event) {
        aggregateCache.evict(event.userId());
    }
}
