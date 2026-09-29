package com.ahorra.feature.behavior.service.impl;

import com.ahorra.feature.accounting.enums.AccountType;
import com.ahorra.feature.accounting.enums.EntryDirection;
import com.ahorra.feature.accounting.model.Category;
import com.ahorra.feature.accounting.repository.CategoryRepository;
import com.ahorra.feature.behavior.BehaviorScope;
import com.ahorra.feature.behavior.calc.BaselineCalculator;
import com.ahorra.feature.behavior.calc.BaselineCalculator.Stats;
import com.ahorra.feature.behavior.calc.HabitDetector;
import com.ahorra.feature.behavior.calc.HabitDetector.HabitSignal;
import com.ahorra.feature.behavior.calc.TrendAnalyzer;
import com.ahorra.feature.behavior.calc.TrendAnalyzer.TrendResult;
import com.ahorra.feature.behavior.dto.CategoryDaySpend;
import com.ahorra.feature.behavior.enums.DayType;
import com.ahorra.feature.behavior.enums.HabitStatus;
import com.ahorra.feature.behavior.model.BehaviorBaseline;
import com.ahorra.feature.behavior.model.DetectedHabit;
import com.ahorra.feature.behavior.repository.BehaviorBaselineRepository;
import com.ahorra.feature.behavior.repository.BehaviorSpendRepository;
import com.ahorra.feature.behavior.repository.DetectedHabitRepository;
import com.ahorra.feature.behavior.service.BehaviorAnalysisService;
import com.ahorra.feature.day.dto.DaySpend;
import com.ahorra.feature.day.service.DailySpendService;
import com.ahorra.feature.accounting.model.IncomeHistory;
import com.ahorra.feature.accounting.repository.IncomeHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BehaviorAnalysisServiceImpl implements BehaviorAnalysisService {

    private static final int[] WINDOWS = {7, 30, 90};
    private static final int MAX_WINDOW = 90;
    private static final int HABIT_WINDOW = 56;
    private static final String UNCATEGORIZED = "Uncategorized";

    private final DailySpendService dailySpendService;
    private final BehaviorSpendRepository behaviorSpendRepository;
    private final BehaviorBaselineRepository baselineRepository;
    private final DetectedHabitRepository habitRepository;
    private final CategoryRepository categoryRepository;
    private final IncomeHistoryRepository incomeHistoryRepository;
    private final BaselineCalculator baselineCalculator;
    private final HabitDetector habitDetector;
    private final TrendAnalyzer trendAnalyzer;

    public BehaviorAnalysisServiceImpl(DailySpendService dailySpendService,
                                       BehaviorSpendRepository behaviorSpendRepository,
                                       BehaviorBaselineRepository baselineRepository,
                                       DetectedHabitRepository habitRepository,
                                       CategoryRepository categoryRepository,
                                       IncomeHistoryRepository incomeHistoryRepository,
                                       BaselineCalculator baselineCalculator,
                                       HabitDetector habitDetector,
                                       TrendAnalyzer trendAnalyzer) {
        this.dailySpendService = dailySpendService;
        this.behaviorSpendRepository = behaviorSpendRepository;
        this.baselineRepository = baselineRepository;
        this.habitRepository = habitRepository;
        this.categoryRepository = categoryRepository;
        this.incomeHistoryRepository = incomeHistoryRepository;
        this.baselineCalculator = baselineCalculator;
        this.habitDetector = habitDetector;
        this.trendAnalyzer = trendAnalyzer;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refresh(Long userId, LocalDate asOf) {
        LocalDate from = asOf.minusDays(MAX_WINDOW - 1L);
        List<DaySpend> closed = dailySpendService.closedDaySpend(userId, from, asOf);
        if (closed.isEmpty()) {
            return;
        }

        Map<Long, Map<LocalDate, BigDecimal>> byCategory = new HashMap<>();
        for (CategoryDaySpend row : behaviorSpendRepository.sumSpendByDayAndCategory(
                userId, AccountType.EXPENSE, EntryDirection.DEBIT, from, asOf)) {
            long key = row.categoryId() == null ? BehaviorScope.UNCATEGORIZED : row.categoryId();
            byCategory.computeIfAbsent(key, k -> new HashMap<>())
                    .merge(row.day(), row.amount(), BigDecimal::add);
        }

        // Zero-filled series over the closed days, so a quiet day counts as a real zero.
        Map<Long, List<DaySpend>> series = new LinkedHashMap<>();
        series.put(BehaviorScope.TOTAL, closed);
        byCategory.forEach((key, perDay) -> series.put(key, closed.stream()
                .map(d -> new DaySpend(d.day(), perDay.getOrDefault(d.day(), BigDecimal.ZERO)))
                .toList()));

        int daysObserved = closed.size();
        series.forEach((key, list) -> updateBaselines(userId, key, list, asOf, daysObserved));
        updateHabits(userId, series, asOf);
    }

    private void updateBaselines(Long userId, long categoryId, List<DaySpend> list,
                                 LocalDate asOf, int daysObserved) {
        BigDecimal confidence = baselineCalculator.confidence(daysObserved);
        for (int window : WINDOWS) {
            LocalDate windowStart = asOf.minusDays(window - 1L);
            for (DayType dayType : DayType.values()) {
                List<BigDecimal> values = list.stream()
                        .filter(d -> !d.day().isBefore(windowStart) && dayType.matches(d.day()))
                        .map(DaySpend::spend)
                        .toList();
                if (values.isEmpty()) {
                    continue;
                }
                Stats stats = baselineCalculator.stats(values);
                BehaviorBaseline row = baselineRepository
                        .findByUserIdAndCategoryIdAndWindowDaysAndDayType(userId, categoryId, window, dayType)
                        .orElseGet(BehaviorBaseline::new);
                row.setUserId(userId);
                row.setCategoryId(categoryId);
                row.setWindowDays(window);
                row.setDayType(dayType);
                row.setMeanAmount(BaselineCalculator.money(stats.mean()));
                row.setStdDev(BaselineCalculator.money(stats.stdDev()));
                row.setSampleDays(stats.n());
                row.setConfidence(confidence);
                row.setComputedThrough(asOf);
                baselineRepository.save(row);
            }
        }
    }

    private void updateHabits(Long userId, Map<Long, List<DaySpend>> series, LocalDate asOf) {
        LocalDate habitStart = asOf.minusDays(HABIT_WINDOW - 1L);
        Set<Long> realIds = series.keySet().stream().filter(k -> k > 0).collect(Collectors.toSet());
        Map<Long, String> names = realIds.isEmpty() ? Map.of()
                : categoryRepository.findAllById(realIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        Set<Long> detected = new HashSet<>();
        series.forEach((key, list) -> {
            if (key == BehaviorScope.TOTAL) {
                return;
            }
            List<DaySpend> recent = list.stream().filter(d -> !d.day().isBefore(habitStart)).toList();
            Optional<HabitSignal> signal = habitDetector.detect(recent);
            if (signal.isEmpty()) {
                return;
            }
            detected.add(key);
            String name = key == BehaviorScope.UNCATEGORIZED ? UNCATEGORIZED
                    : names.getOrDefault(key, UNCATEGORIZED);
            saveHabit(userId, key, name, signal.get());
        });

        // A habit that no longer meets the rules is kept, but marked lapsed.
        for (DetectedHabit existing : habitRepository.findAllByUserId(userId)) {
            if (!detected.contains(existing.getCategoryId()) && existing.getStatus() == HabitStatus.ACTIVE) {
                existing.setStatus(HabitStatus.LAPSED);
                habitRepository.save(existing);
            }
        }
    }

    private void saveHabit(Long userId, long categoryId, String name, HabitSignal s) {
        DetectedHabit habit = habitRepository.findByUserIdAndCategoryId(userId, categoryId)
                .orElseGet(() -> {
                    DetectedHabit created = new DetectedHabit();
                    created.setUserId(userId);
                    created.setCategoryId(categoryId);
                    created.setFirstObserved(s.firstObserved());
                    return created;
                });
        habit.setDescription(String.format(Locale.US, "%s: about %.1f times a week, averaging \u20A6%,.0f",
                name, s.frequencyPerWeek().doubleValue(), s.avgAmount().doubleValue()));
        habit.setLastObserved(s.lastObserved());
        habit.setFrequencyPerWeek(s.frequencyPerWeek());
        habit.setAvgAmount(s.avgAmount());
        habit.setTrendDirection(s.trend());
        habit.setConfidence(s.confidence());
        habit.setStatus(HabitStatus.ACTIVE);
        habitRepository.save(habit);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrendResult> trend(Long userId, LocalDate asOf) {
        Map<LocalDate, BigDecimal> byDay = dailySpendService
                .closedDaySpend(userId, asOf.minusDays(55), asOf).stream()
                .collect(Collectors.toMap(DaySpend::day, DaySpend::spend));

        List<Double> ratios = new ArrayList<>();
        for (int k = 7; k >= 0; k--) {
            LocalDate end = asOf.minusDays(7L * k);
            LocalDate start = end.minusDays(6);
            BigDecimal sum = BigDecimal.ZERO;
            boolean complete = true;
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                BigDecimal v = byDay.get(d);
                if (v == null) {
                    complete = false;
                    break;
                }
                sum = sum.add(v);
            }
            if (!complete) {
                continue;
            }
            Optional<IncomeHistory> income = incomeHistoryRepository.findActiveOn(userId, end);
            if (income.isEmpty()) {
                continue;
            }
            double weeklyBaseline = weeklyIncomeBaseline(income.get());
            if (weeklyBaseline <= 0) {
                continue;
            }
            ratios.add(sum.doubleValue() / weeklyBaseline * 100.0);
        }
        return trendAnalyzer.analyze(ratios);
    }

    /** The lower-bound income for a period, scaled to one week. */
    private double weeklyIncomeBaseline(IncomeHistory income) {
        double periodDays = switch (income.getPeriodType()) {
            case DAILY -> 1;
            case WEEKLY -> 7;
            case MONTHLY -> 365.25 / 12;
            case CUSTOM -> income.getPeriodLength();
        };
        return income.getMinAmount().doubleValue() / periodDays * 7;
    }
}
