package com.ahorra.feature.report.listener;

import com.ahorra.feature.day.event.DayClosedEvent;
import com.ahorra.feature.report.service.DailyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Generates the day's report once the close has committed, so the model call
 * never runs inside (or delays) the day-close transaction. A failure here is
 * logged and left for the nightly backfill to pick up.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StandardReportListener {

    private final DailyReportService dailyReportService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDayClosed(DayClosedEvent event) {
        try {
            dailyReportService.generateIfMissing(event.userId(), event.day());
        } catch (RuntimeException ex) {
            log.error("Report generation failed for user {} on {}", event.userId(), event.day(), ex);
        }
    }
}
