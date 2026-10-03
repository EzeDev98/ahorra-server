package com.ahorra.feature.plan.service.impl;

import com.ahorra.common.money.MoneyUtils;
import com.ahorra.feature.accounting.enums.PeriodType;
import com.ahorra.feature.accounting.model.IncomeHistory;
import com.ahorra.feature.accounting.repository.IncomeHistoryRepository;
import com.ahorra.feature.income.dto.CategoryBreakdown;
import com.ahorra.feature.income.dto.VarianceResponse;
import com.ahorra.feature.income.service.VarianceService;
import com.ahorra.feature.obligation.enums.ObligationCadence;
import com.ahorra.feature.obligation.model.Obligation;
import com.ahorra.feature.obligation.repository.ObligationRepository;
import com.ahorra.feature.plan.dto.PlanBucket;
import com.ahorra.feature.plan.dto.PlanResponse;
import com.ahorra.feature.plan.enums.PlanPhase;
import com.ahorra.feature.plan.service.PlanService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.ahorra.feature.plan.service.impl.PlanPolicy.*;

/**
 * Splits the lower bound of the person's income 50/30/20 over the income period. Fixed obligations are taken out of
 * the Needs share, so what is left of it is the room for everyday needs. From day 30 the split seen in confirmed
 * entries is returned beside the targets; the targets themselves never move.
 */
@Service
public class PlanServiceImpl implements PlanService {

    /** Average days in a month, used to turn a monthly obligation into a period amount when the period is not a month. */
    private static final BigDecimal AVERAGE_MONTH_DAYS = new BigDecimal("30.4375");

    private final VarianceService varianceService;
    private final ObligationRepository obligationRepository;
    private final IncomeHistoryRepository incomeHistoryRepository;
    private final ZoneId zone;

    public PlanServiceImpl(VarianceService varianceService,
                           ObligationRepository obligationRepository,
                           IncomeHistoryRepository incomeHistoryRepository,
                           @Value("${app.timezone:Africa/Lagos}") String timezone) {
        this.varianceService = varianceService;
        this.obligationRepository = obligationRepository;
        this.incomeHistoryRepository = incomeHistoryRepository;
        this.zone = ZoneId.of(timezone);
    }

    @Override
    public PlanResponse currentPlan(Long userId) {
        LocalDate today = LocalDate.now(zone);
        VarianceResponse variance = varianceService.variance(userId, today);

        BigDecimal basis = MoneyUtils.normalize(variance.baseline());
        BigDecimal obligations = obligationsForPeriod(userId, variance.periodType(), variance.daysInPeriod());

        int planDay = (int) ChronoUnit.DAYS.between(accountStart(userId, today), today) + 1;
        PlanPhase phase = planDay > STARTER_DAYS ? PlanPhase.OBSERVED : PlanPhase.STARTER;

        BigDecimal needsTarget = MoneyUtils.percentage(basis, BigDecimal.valueOf(NEEDS_PERCENT));
        BigDecimal wantsTarget = MoneyUtils.percentage(basis, BigDecimal.valueOf(WANTS_PERCENT));
        BigDecimal savingsTarget = MoneyUtils.subtract(MoneyUtils.subtract(basis, needsTarget), wantsTarget);

        BigDecimal needsSpent = BigDecimal.ZERO;
        BigDecimal wantsSpent = BigDecimal.ZERO;
        for (CategoryBreakdown line : variance.categories()) {
            if (NEEDS_CATEGORIES.contains(line.categoryName())) {
                needsSpent = MoneyUtils.add(needsSpent, line.amount());
            } else {
                wantsSpent = MoneyUtils.add(wantsSpent, line.amount());
            }
        }
        BigDecimal unspent = MoneyUtils.max(MoneyUtils.subtract(basis, variance.spend()), BigDecimal.ZERO);

        boolean observed = phase == PlanPhase.OBSERVED;
        List<PlanBucket> buckets = List.of(
                new PlanBucket("NEEDS", NEEDS_PERCENT, needsTarget, obligations,
                        MoneyUtils.subtract(needsTarget, obligations), observed ? MoneyUtils.normalize(needsSpent) : null),
                new PlanBucket("WANTS", WANTS_PERCENT, wantsTarget, MoneyUtils.normalize(BigDecimal.ZERO),
                        wantsTarget, observed ? MoneyUtils.normalize(wantsSpent) : null),
                new PlanBucket("SAVINGS", SAVINGS_PERCENT, savingsTarget, MoneyUtils.normalize(BigDecimal.ZERO),
                        savingsTarget, observed ? unspent : null)
        );

        return new PlanResponse(phase, planDay, STARTER_DAYS, variance.periodType(), variance.periodStart(),
                variance.periodEnd(), variance.daysElapsed(), variance.daysInPeriod(), basis, obligations,
                MoneyUtils.isGreaterThan(obligations, needsTarget), buckets);
    }

    /** Active obligations, each turned into what falls due over one income period. */
    private BigDecimal obligationsForPeriod(Long userId, PeriodType periodType, int daysInPeriod) {
        BigDecimal days = BigDecimal.valueOf(daysInPeriod);
        // A calendar-month period holds exactly one monthly obligation, however many days the month has.
        BigDecimal monthDays = periodType == PeriodType.MONTHLY ? days : AVERAGE_MONTH_DAYS;

        BigDecimal total = BigDecimal.ZERO;
        for (Obligation obligation : obligationRepository.findAllByUserIdOrderByIdAsc(userId)) {
            if (!obligation.isActive()) {
                continue;
            }
            BigDecimal cadenceDays = cadenceDays(obligation.getCadence(), monthDays);
            BigDecimal perPeriod = obligation.getAmount().multiply(days).divide(cadenceDays, 2, RoundingMode.HALF_UP);
            total = MoneyUtils.add(total, perPeriod);
        }
        return MoneyUtils.normalize(total);
    }

    private BigDecimal cadenceDays(ObligationCadence cadence, BigDecimal monthDays) {
        return switch (cadence) {
            case DAILY -> BigDecimal.ONE;
            case WEEKLY -> BigDecimal.valueOf(7);
            case MONTHLY -> monthDays;
        };
    }

    /** The day the account started: the start of the earliest income record, which is set to the registration day. */
    private LocalDate accountStart(Long userId, LocalDate today) {
        List<IncomeHistory> history = incomeHistoryRepository.findAllByUserIdOrderByEffectiveFromDesc(userId);
        if (history.isEmpty()) {
            throw new EntityNotFoundException("No income has been set yet");
        }
        LocalDate start = history.get(history.size() - 1).getEffectiveFrom();
        return start.isAfter(today) ? today : start;
    }
}
