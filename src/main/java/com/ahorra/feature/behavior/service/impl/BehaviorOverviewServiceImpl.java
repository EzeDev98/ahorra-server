package com.ahorra.feature.behavior.service.impl;

import com.ahorra.feature.accounting.enums.AccountType;
import com.ahorra.feature.accounting.enums.EntryDirection;
import com.ahorra.feature.accounting.model.Category;
import com.ahorra.feature.accounting.repository.CategoryRepository;
import com.ahorra.feature.accounting.repository.LedgerEntryRepository;
import com.ahorra.feature.behavior.BehaviorScope;
import com.ahorra.feature.behavior.calc.BaselineCalculator;
import com.ahorra.feature.behavior.calc.BaselineCalculator.Stats;
import com.ahorra.feature.behavior.calc.TrendAnalyzer.TrendResult;
import com.ahorra.feature.behavior.dto.BaselineView;
import com.ahorra.feature.behavior.dto.BehaviorOverview;
import com.ahorra.feature.behavior.dto.HabitView;
import com.ahorra.feature.behavior.dto.TrendView;
import com.ahorra.feature.behavior.enums.BehaviorMode;
import com.ahorra.feature.behavior.enums.DayType;
import com.ahorra.feature.behavior.enums.HabitStatus;
import com.ahorra.feature.behavior.enums.TrendDirection;
import com.ahorra.feature.behavior.model.BehaviorBaseline;
import com.ahorra.feature.behavior.repository.BehaviorBaselineRepository;
import com.ahorra.feature.behavior.repository.DetectedHabitRepository;
import com.ahorra.feature.behavior.service.BehaviorAnalysisService;
import com.ahorra.feature.behavior.service.BehaviorOverviewService;
import com.ahorra.feature.day.dto.DaySpend;
import com.ahorra.feature.day.service.DailySpendService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BehaviorOverviewServiceImpl implements BehaviorOverviewService {

    /** Closed days needed before judgments switch on. */
    private static final int JUDGMENT_DAYS = 30;
    private static final int LOOKBACK_DAYS = 90;
    private static final double UNUSUAL_Z = 2.0;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final String ALL_SPENDING = "All spending";
    private static final String UNCATEGORIZED = "Uncategorized";

    private final DailySpendService dailySpendService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BehaviorBaselineRepository baselineRepository;
    private final DetectedHabitRepository habitRepository;
    private final CategoryRepository categoryRepository;
    private final BehaviorAnalysisService analysisService;
    private final BaselineCalculator baselineCalculator;
    private final ZoneId zone;

    public BehaviorOverviewServiceImpl(DailySpendService dailySpendService,
                                       LedgerEntryRepository ledgerEntryRepository,
                                       BehaviorBaselineRepository baselineRepository,
                                       DetectedHabitRepository habitRepository,
                                       CategoryRepository categoryRepository,
                                       BehaviorAnalysisService analysisService,
                                       BaselineCalculator baselineCalculator,
                                       @Value("${app.timezone:Africa/Lagos}") String timezone) {
        this.dailySpendService = dailySpendService;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.baselineRepository = baselineRepository;
        this.habitRepository = habitRepository;
        this.categoryRepository = categoryRepository;
        this.analysisService = analysisService;
        this.baselineCalculator = baselineCalculator;
        this.zone = ZoneId.of(timezone);
    }

    @Override
    @Transactional(readOnly = true)
    public BehaviorOverview overview(Long userId) {
        LocalDate today = LocalDate.now(zone);
        LocalDate lastClosed = today.minusDays(1);

        List<DaySpend> closed = dailySpendService.closedDaySpend(
                userId, today.minusDays(LOOKBACK_DAYS), lastClosed);
        int daysObserved = closed.size();
        BehaviorMode mode = daysObserved < JUDGMENT_DAYS ? BehaviorMode.OBSERVATION : BehaviorMode.JUDGMENT;
        int daysUntil = Math.max(0, JUDGMENT_DAYS - daysObserved);
        BigDecimal confidence = baselineCalculator.confidence(daysObserved);

        List<BehaviorBaseline> baselineRows = baselineRepository.findAllByUserId(userId);
        Map<Long, String> names = categoryNames(baselineRows);
        List<BaselineView> baselines = baselineRows.stream()
                .sorted(Comparator.comparing((BehaviorBaseline b) -> b.getCategoryId() != BehaviorScope.TOTAL)
                        .thenComparing(BehaviorBaseline::getCategoryId)
                        .thenComparing(BehaviorBaseline::getWindowDays)
                        .thenComparing(BehaviorBaseline::getDayType))
                .map(b -> new BaselineView(
                        nameOf(b.getCategoryId(), names), b.getWindowDays(), b.getDayType(),
                        b.getMeanAmount(), b.getStdDev(), b.getSampleDays(),
                        b.getConfidence(), b.getComputedThrough()))
                .toList();

        if (mode == BehaviorMode.OBSERVATION) {
            return new BehaviorOverview(today, mode, daysObserved, daysUntil, confidence,
                    baselines, List.of(), null, null, null);
        }

        List<HabitView> habits = habitRepository.findAllByUserIdAndStatus(userId, HabitStatus.ACTIVE).stream()
                .sorted(Comparator.comparing(h -> h.getConfidence(), Comparator.reverseOrder()))
                .map(h -> new HabitView(h.getDescription(), h.getFrequencyPerWeek(), h.getAvgAmount(),
                        h.getFirstObserved(), h.getLastObserved(), h.getTrendDirection(), h.getConfidence()))
                .toList();

        TrendView trend = analysisService.trend(userId, lastClosed)
                .map(this::toView)
                .orElse(null);

        BigDecimal zScore = null;
        Boolean unusual = null;
        Optional<BehaviorBaseline> usual = baselineRepository
                .findByUserIdAndCategoryIdAndWindowDaysAndDayType(
                        userId, BehaviorScope.TOTAL, 30, dayTypeOf(today));
        if (usual.isPresent()) {
            BigDecimal spendToday = ledgerEntryRepository
                    .sumSpendByDay(userId, AccountType.EXPENSE, EntryDirection.DEBIT, today, today)
                    .stream().map(DaySpend::spend).reduce(BigDecimal.ZERO, BigDecimal::add);
            BehaviorBaseline b = usual.get();
            Double z = baselineCalculator.zScore(spendToday,
                    new Stats(b.getMeanAmount().doubleValue(), b.getStdDev().doubleValue(), b.getSampleDays()));
            if (z != null) {
                zScore = BaselineCalculator.money(z);
                unusual = z >= UNUSUAL_Z;
            }
        }

        return new BehaviorOverview(today, mode, daysObserved, daysUntil, confidence,
                baselines, habits, trend, zScore, unusual);
    }

    private TrendView toView(TrendResult r) {
        boolean above = r.direction() == TrendDirection.WORSENING
                && r.latestRatioPercent().compareTo(HUNDRED) >= 0;
        return new TrendView(r.direction(), r.slopePerWeek(), r.latestRatioPercent(), r.weeksUsed(), above);
    }

    /** The day type whose baseline row matches this calendar day (weekday or weekend). */
    private DayType dayTypeOf(LocalDate day) {
        return DayType.WEEKEND.matches(day) ? DayType.WEEKEND : DayType.WEEKDAY;
    }

    private Map<Long, String> categoryNames(List<BehaviorBaseline> rows) {
        Set<Long> ids = rows.stream().map(BehaviorBaseline::getCategoryId)
                .filter(id -> id > 0).collect(Collectors.toSet());
        return ids.isEmpty() ? Map.of()
                : categoryRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private String nameOf(Long categoryId, Map<Long, String> names) {
        if (categoryId == BehaviorScope.TOTAL) {
            return ALL_SPENDING;
        }
        if (categoryId == BehaviorScope.UNCATEGORIZED) {
            return UNCATEGORIZED;
        }
        return names.getOrDefault(categoryId, UNCATEGORIZED);
    }
}
