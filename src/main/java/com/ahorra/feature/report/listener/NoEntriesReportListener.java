package com.ahorra.feature.report.listener;

import com.ahorra.feature.accounting.repository.LedgerEntryRepository;
import com.ahorra.feature.day.event.DayClosedEvent;
import com.ahorra.feature.report.enums.ReportKind;
import com.ahorra.feature.report.model.DailyReport;
import com.ahorra.feature.report.repository.DailyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Stores the "No entries logged today" record when a day closes with nothing
 * in the ledger. Deliberately a plain (synchronous) listener, not an
 * after-commit one: it runs inside the transaction that closes the day, so a
 * day can never close without its record.
 *
 * <p>The wording states an absence of logging, never a spend total: the app
 * only knows nothing was entered, not that nothing was spent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoEntriesReportListener {

    static final String NO_ENTRIES_SUMMARY = "No entries logged today";

    private final LedgerEntryRepository ledgerEntryRepository;
    private final DailyReportRepository dailyReportRepository;

    @EventListener
    public void onDayClosed(DayClosedEvent event) {
        // A retroactive close means the day had pending entries, so it has entries.
        if (event.retroactive()) {
            return;
        }
        if (ledgerEntryRepository.countByUserIdAndEntryDate(event.userId(), event.day()) > 0) {
            return;
        }
        if (dailyReportRepository.existsByUserIdAndReportDate(event.userId(), event.day())) {
            return;
        }

        DailyReport report = new DailyReport();
        report.setUserId(event.userId());
        report.setReportDate(event.day());
        report.setKind(ReportKind.NO_ENTRIES);
        report.setSummaryText(NO_ENTRIES_SUMMARY);
        dailyReportRepository.save(report);
        log.info("Stored no-entries report for user {} on {}", event.userId(), event.day());
    }
}
