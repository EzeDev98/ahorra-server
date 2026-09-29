package com.ahorra.feature.report.service.impl;

import com.ahorra.feature.accounting.enums.AccountType;
import com.ahorra.feature.accounting.enums.EntryDirection;
import com.ahorra.feature.accounting.repository.LedgerEntryRepository;
import com.ahorra.feature.capture.enums.FlowDirection;
import com.ahorra.feature.capture.enums.PendingStatus;
import com.ahorra.feature.capture.repository.PendingEntryRepository;
import com.ahorra.feature.day.dto.DaySpend;
import com.ahorra.feature.day.service.DayGateService;
import com.ahorra.feature.income.dto.VarianceResponse;
import com.ahorra.feature.income.service.VarianceService;
import com.ahorra.feature.report.dto.AmbientResponse;
import com.ahorra.feature.report.enums.ReportVerdict;
import com.ahorra.feature.report.service.AmbientAggregateCache;
import com.ahorra.feature.report.service.AmbientAggregateCache.UsualSpend;
import com.ahorra.feature.report.service.AmbientService;
import com.ahorra.feature.report.service.VerdictCalculator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class AmbientServiceImpl implements AmbientService {

    private final DayGateService dayGateService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PendingEntryRepository pendingEntryRepository;
    private final AmbientAggregateCache aggregateCache;
    private final VarianceService varianceService;
    private final VerdictCalculator verdictCalculator;
    private final ZoneId zone;

    public AmbientServiceImpl(DayGateService dayGateService,
                              LedgerEntryRepository ledgerEntryRepository,
                              PendingEntryRepository pendingEntryRepository,
                              AmbientAggregateCache aggregateCache,
                              VarianceService varianceService,
                              VerdictCalculator verdictCalculator,
                              @Value("${app.timezone:Africa/Lagos}") String timezone) {
        this.dayGateService = dayGateService;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.pendingEntryRepository = pendingEntryRepository;
        this.aggregateCache = aggregateCache;
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

        // Precomputed once a day and cached; confirmed and pending above stay live.
        UsualSpend usual = aggregateCache.usualSpend(userId, today);

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
                pendingCount > 0, usual.usualDailySpend(), usual.daysObserved(),
                usual.comparisonAvailable(), verdict, periodVariance, periodBaseline);
    }
}
