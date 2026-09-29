package com.ahorra.feature.report.service.impl;

import com.ahorra.feature.accounting.enums.AccountType;
import com.ahorra.feature.accounting.enums.EntryDirection;
import com.ahorra.feature.accounting.repository.LedgerEntryRepository;
import com.ahorra.feature.capture.enums.FlowDirection;
import com.ahorra.feature.capture.enums.PendingStatus;
import com.ahorra.feature.capture.repository.PendingEntryRepository;
import com.ahorra.feature.day.dto.DaySpend;
import com.ahorra.feature.day.service.DailySpendService;
import com.ahorra.feature.day.service.DayGateService;
import com.ahorra.feature.income.dto.VarianceResponse;
import com.ahorra.feature.income.service.VarianceService;
import com.ahorra.feature.report.dto.AmbientResponse;
import com.ahorra.feature.report.enums.ReportVerdict;
import com.ahorra.feature.report.service.AmbientService;
import com.ahorra.feature.report.service.VerdictCalculator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class AmbientServiceImpl implements AmbientService {

    /** Days of closed history needed before "your usual" is shown. */
    private static final int WINDOW_DAYS = 30;

    private final DayGateService dayGateService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PendingEntryRepository pendingEntryRepository;
    private final DailySpendService dailySpendService;
    private final VarianceService varianceService;
    private final VerdictCalculator verdictCalculator;
    private final ZoneId zone;

    public AmbientServiceImpl(DayGateService dayGateService,
                              LedgerEntryRepository ledgerEntryRepository,
                              PendingEntryRepository pendingEntryRepository,
                              DailySpendService dailySpendService,
                              VarianceService varianceService,
                              VerdictCalculator verdictCalculator,
                              @Value("${app.timezone:Africa/Lagos}") String timezone) {
        this.dayGateService = dayGateService;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.pendingEntryRepository = pendingEntryRepository;
        this.dailySpendService = dailySpendService;
        this.varianceService = varianceService;
        this.verdictCalculator = verdictCalculator;
        this.zone = ZoneId.of(timezone);
    }

    @Override
    @Transactional(readOnly = true)
    public AmbientResponse today(Long userId) {
        dayGateService.assertNotGated(userId);
        LocalDate today = LocalDate.now(zone);

        BigDecimal confirmed = ledgerEntryRepository
                .sumSpendByDay(userId, AccountType.EXPENSE, EntryDirection.DEBIT, today, today)
                .stream()
                .map(DaySpend::spend)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pending = pendingEntryRepository.sumTotalByStatusAndFlowAndDay(
                userId, PendingStatus.PENDING, FlowDirection.EXPENSE, today);
        if (pending == null) {
            pending = BigDecimal.ZERO;
        }
        long pendingCount = pendingEntryRepository
                .countByUserIdAndStatusAndEntryDate(userId, PendingStatus.PENDING, today);

        BigDecimal provisional = confirmed.add(pending);

        // "Your usual" only appears once a full window of closed days exists.
        List<DaySpend> history = dailySpendService.closedDaySpend(
                userId, today.minusDays(WINDOW_DAYS), today.minusDays(1));
        boolean comparisonAvailable = history.size() >= WINDOW_DAYS;
        BigDecimal usual = null;
        if (comparisonAvailable) {
            BigDecimal total = history.stream()
                    .map(DaySpend::spend)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            usual = total.divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);
        }

        // Verdict and variance use confirmed spend only.
        ReportVerdict verdict;
        BigDecimal periodVariance = null;
        BigDecimal periodBaseline = null;
        try {
            VarianceResponse v = varianceService.variance(userId, today);
            verdict = verdictCalculator.verdict(v);
            periodVariance = v.variance();
            periodBaseline = v.baseline();
        } catch (EntityNotFoundException e) {
            verdict = ReportVerdict.NO_INCOME_SET;
        }

        return new AmbientResponse(
                today, confirmed, pending, (int) pendingCount, provisional,
                pendingCount > 0, usual, history.size(), comparisonAvailable,
                verdict, periodVariance, periodBaseline);
    }
}
