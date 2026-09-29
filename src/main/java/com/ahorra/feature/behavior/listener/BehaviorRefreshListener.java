package com.ahorra.feature.behavior.listener;

import com.ahorra.feature.behavior.service.BehaviorAnalysisService;
import com.ahorra.feature.day.event.DayClosedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Refreshes the user's baselines and habits once a day-close has committed. A
 * failure here is logged and never affects the close itself.
 */
@Component
public class BehaviorRefreshListener {

    private static final Logger log = LoggerFactory.getLogger(BehaviorRefreshListener.class);

    private final BehaviorAnalysisService analysisService;

    public BehaviorRefreshListener(BehaviorAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDayClosed(DayClosedEvent event) {
        try {
            analysisService.refresh(event.userId(), event.day());
        } catch (Exception e) {
            log.warn("Behavior refresh failed for user {} through {}", event.userId(), event.day(), e);
        }
    }
}
