package com.ahorra.feature.report.service.impl;

import com.ahorra.common.money.MoneyUtils;
import com.ahorra.feature.accounting.enums.AccountType;
import com.ahorra.feature.accounting.enums.EntryDirection;
import com.ahorra.feature.accounting.model.Category;
import com.ahorra.feature.accounting.repository.CategoryRepository;
import com.ahorra.feature.accounting.repository.LedgerEntryRepository;
import com.ahorra.feature.day.dto.DaySpend;
import com.ahorra.feature.day.enums.DayStatus;
import com.ahorra.feature.day.repository.DayStateRepository;
import com.ahorra.feature.day.service.DailySpendService;
import com.ahorra.feature.income.dto.CategorySpendRow;
import com.ahorra.feature.income.dto.VarianceResponse;
import com.ahorra.feature.income.service.VarianceService;
import com.ahorra.feature.report.dto.CategoryLine;
import com.ahorra.feature.report.dto.ReportFacts;
import com.ahorra.feature.report.enums.CategoryScope;
import com.ahorra.feature.report.enums.NarrativeSource;
import com.ahorra.feature.report.enums.ReportKind;
import com.ahorra.feature.report.enums.ReportVerdict;
import com.ahorra.feature.report.model.DailyReport;
import com.ahorra.feature.report.model.DailyReportCategory;
import com.ahorra.feature.report.repository.DailyReportRepository;
import com.ahorra.feature.report.service.DailyReportService;
import com.ahorra.feature.report.service.NarrativeGuard;
import com.ahorra.feature.report.service.NarrativeWriter;
import com.ahorra.feature.report.service.ReportFormat;
import com.ahorra.feature.report.service.VerdictCalculator;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Not @Transactional as a whole, on purpose: the model call is slow and must not
 * hold a database connection. Reads run normally and the final write runs in its
 * own new transaction, which also makes it commit correctly when this is called
 * from an after-commit listener.
 */
@Slf4j
@Service
public class DailyReportServiceImpl implements DailyReportService {

    private static final String NO_ENTRIES_SUMMARY = "No entries logged today";
    private static final int OBSERVATION_DAYS = 30;
    private static final int MAX_BACKFILL_DAYS = 14;
    private static final int MAX_INSIGHT_LENGTH = 500;

    private final DailyReportRepository reportRepository;
    private final DayStateRepository dayStateRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final CategoryRepository categoryRepository;
    private final VarianceService varianceService;
    private final DailySpendService dailySpendService;
    private final VerdictCalculator verdictCalculator;
    private final NarrativeWriter narrativeWriter;
    private final TemplateNarrative templateNarrative;
    private final TransactionTemplate requiresNew;

    public DailyReportServiceImpl(DailyReportRepository reportRepository,
                                  DayStateRepository dayStateRepository,
                                  LedgerEntryRepository ledgerEntryRepository,
                                  CategoryRepository categoryRepository,
                                  VarianceService varianceService,
                                  DailySpendService dailySpendService,
                                  VerdictCalculator verdictCalculator,
                                  NarrativeWriter narrativeWriter,
                                  TemplateNarrative templateNarrative,
                                  PlatformTransactionManager transactionManager) {
        this.reportRepository = reportRepository;
        this.dayStateRepository = dayStateRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.categoryRepository = categoryRepository;
        this.varianceService = varianceService;
        this.dailySpendService = dailySpendService;
        this.verdictCalculator = verdictCalculator;
        this.narrativeWriter = narrativeWriter;
        this.templateNarrative = templateNarrative;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void generateMissing(Long userId) {
        List<LocalDate> days = dayStateRepository.findDaysWithoutReport(userId, DayStatus.CLOSED);
        for (LocalDate day : days.stream().limit(MAX_BACKFILL_DAYS).toList()) {
            try {
                generateIfMissing(userId, day);
            } catch (RuntimeException ex) {
                log.error("Report backfill failed for user {} on {}", userId, day, ex);
            }
        }
    }

    @Override
    public void generateIfMissing(Long userId, LocalDate day) {
        if (reportRepository.existsByUserIdAndReportDate(userId, day)) {
            return;
        }
        // Gated on the day-close: no report for a day that has not closed.
        if (!dayStateRepository.existsByUserIdAndDayDateAndStatus(userId, day, DayStatus.CLOSED)) {
            return;
        }

        try {
            if (ledgerEntryRepository.countByUserIdAndEntryDate(userId, day) == 0) {
                saveNoEntries(userId, day);
                return;
            }

            ReportFacts facts = buildFacts(userId, day);
            String narrative = null;
            NarrativeSource source = NarrativeSource.TEMPLATE;
            try {
                String candidate = narrativeWriter.write(facts);
                if (NarrativeGuard.isAcceptable(candidate, facts)) {
                    narrative = candidate;
                    source = NarrativeSource.AI;
                } else {
                    log.warn("Narrative for user {} on {} failed the number check; using template", userId, day);
                }
            } catch (RuntimeException ex) {
                log.warn("Narrative writer unavailable for user {} on {}: {}", userId, day, ex.getMessage());
            }
            if (narrative == null) {
                narrative = templateNarrative.write(facts);
            }
            persist(userId, facts, narrative, source);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Report for user {} on {} was created concurrently", userId, day);
        }
    }

    private ReportFacts buildFacts(Long userId, LocalDate day) {
        List<CategorySpendRow> rows = ledgerEntryRepository.sumSpendByCategory(
                userId, AccountType.EXPENSE, EntryDirection.DEBIT, day, day);
        BigDecimal daySpend = rows.stream().map(CategorySpendRow::amount)
                .reduce(BigDecimal.ZERO, MoneyUtils::add);
        int entryCount = (int) ledgerEntryRepository.countByUserIdAndEntryDate(userId, day);
        List<CategoryLine> dayLines = dayLines(rows, daySpend);

        VarianceResponse variance = null;
        try {
            variance = varianceService.variance(userId, day);
        } catch (EntityNotFoundException ex) {
            // No income was set for that day; the report simply has nothing to compare against.
        }
        ReportVerdict verdict = variance == null ? ReportVerdict.NO_INCOME_SET : verdictCalculator.verdict(variance);

        List<DaySpend> history = dailySpendService.closedDaySpend(
                userId, day.minusDays(OBSERVATION_DAYS), day.minusDays(1));
        int daysObserved = history.size();
        BigDecimal rollingAvg = daysObserved == 0 ? null : MoneyUtils.divide(
                history.stream().map(DaySpend::spend).reduce(BigDecimal.ZERO, MoneyUtils::add),
                BigDecimal.valueOf(daysObserved));
        boolean comparisonAvailable = daysObserved >= OBSERVATION_DAYS;

        boolean goodDay = verdict == ReportVerdict.UNDER
                && (!comparisonAvailable || MoneyUtils.isLessThan(daySpend, rollingAvg));

        List<CategoryLine> periodLines = variance == null ? List.of() : variance.categories().stream()
                .map(c -> new CategoryLine(c.categoryName(), c.amount(), c.shareOfSpendPercent(), c.attributedExcess()))
                .toList();

        return new ReportFacts(
                day, entryCount, MoneyUtils.normalize(daySpend),
                variance == null ? null : variance.periodType(),
                variance == null ? null : variance.periodStart(),
                variance == null ? null : variance.periodEnd(),
                variance == null ? 0 : variance.daysElapsed(),
                variance == null ? 0 : variance.daysInPeriod(),
                variance == null ? null : variance.baseline(),
                variance == null ? null : variance.incomeMax(),
                variance == null ? null : variance.spend(),
                variance == null ? null : variance.variance(),
                rollingAvg, daysObserved, comparisonAvailable, verdict,
                buildInsight(variance, periodLines, dayLines, daySpend),
                goodDay, dayLines, periodLines);
    }

    private List<CategoryLine> dayLines(List<CategorySpendRow> rows, BigDecimal daySpend) {
        Set<Long> ids = rows.stream().map(CategorySpendRow::categoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> names = ids.isEmpty() ? Map.of()
                : categoryRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return rows.stream()
                .sorted(Comparator.comparing(CategorySpendRow::amount).reversed())
                .map(row -> {
                    BigDecimal amount = MoneyUtils.normalize(row.amount());
                    String name = row.categoryId() == null ? "Uncategorized"
                            : names.getOrDefault(row.categoryId(), "Uncategorized");
                    BigDecimal share = MoneyUtils.isPositive(daySpend)
                            ? MoneyUtils.divide(MoneyUtils.multiply(amount, new BigDecimal("100")), daySpend)
                            : MoneyUtils.normalize(BigDecimal.ZERO);
                    return new CategoryLine(name, amount, share, null);
                })
                .toList();
    }

    /** One deterministic line: the biggest driver of the overspend if over, otherwise the day's biggest category. */
    private String buildInsight(VarianceResponse variance, List<CategoryLine> periodLines,
                                List<CategoryLine> dayLines, BigDecimal daySpend) {
        if (variance != null && MoneyUtils.isPositive(variance.variance()) && !periodLines.isEmpty()) {
            CategoryLine top = periodLines.get(0);
            if (MoneyUtils.isPositive(top.attributedExcess())) {
                return truncate(top.name() + " accounts for " + ReportFormat.money(top.attributedExcess())
                        + " of the " + ReportFormat.money(variance.variance())
                        + " by which spending is above the income baseline this period.", MAX_INSIGHT_LENGTH);
            }
        }
        if (MoneyUtils.isPositive(daySpend) && !dayLines.isEmpty()) {
            CategoryLine top = dayLines.get(0);
            return truncate(top.name() + " was " + ReportFormat.percent(top.sharePercent())
                    + " of the day's spending.", MAX_INSIGHT_LENGTH);
        }
        return null;
    }

    private void persist(Long userId, ReportFacts f, String narrative, NarrativeSource source) {
        DailyReport report = new DailyReport();
        report.setUserId(userId);
        report.setReportDate(f.day());
        report.setKind(ReportKind.STANDARD);
        report.setSummaryText(narrative);
        report.setPeriodType(f.periodType());
        report.setPeriodStart(f.periodStart());
        report.setPeriodEnd(f.periodEnd());
        report.setEntryCount(f.entryCount());
        report.setDaySpend(f.daySpend());
        report.setBaseline(f.baseline());
        report.setIncomeMax(f.incomeMax());
        report.setPeriodSpend(f.periodSpend());
        report.setVariance(f.variance());
        report.setRollingAvgDailySpend(f.rollingAvg());
        report.setDaysObserved(f.daysObserved());
        report.setVerdict(f.verdict());
        report.setInsightText(f.insight());
        report.setNarrativeSource(source);
        addCategories(report, CategoryScope.DAY, f.dayCategories());
        addCategories(report, CategoryScope.PERIOD, f.periodCategories());

        requiresNew.executeWithoutResult(status -> reportRepository.save(report));
        log.info("Stored {} report for user {} on {}", source, userId, f.day());
    }

    private void addCategories(DailyReport report, CategoryScope scope, List<CategoryLine> lines) {
        int position = 0;
        for (CategoryLine line : lines) {
            DailyReportCategory row = new DailyReportCategory();
            row.setScope(scope);
            row.setPosition(position++);
            row.setCategoryName(line.name());
            row.setAmount(line.amount());
            row.setSharePercent(line.sharePercent());
            row.setAttributedExcess(line.attributedExcess());
            report.addCategory(row);
        }
    }

    private void saveNoEntries(Long userId, LocalDate day) {
        DailyReport report = new DailyReport();
        report.setUserId(userId);
        report.setReportDate(day);
        report.setKind(ReportKind.NO_ENTRIES);
        report.setSummaryText(NO_ENTRIES_SUMMARY);
        report.setEntryCount(0);
        requiresNew.executeWithoutResult(status -> reportRepository.save(report));
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
