package com.ahorra.feature.report.service.impl;

import com.ahorra.feature.report.dto.CategoryLine;
import com.ahorra.feature.report.dto.DailyReportResponse;
import com.ahorra.feature.report.dto.DailyReportSummary;
import com.ahorra.feature.report.enums.CategoryScope;
import com.ahorra.feature.report.model.DailyReport;
import com.ahorra.feature.report.model.DailyReportCategory;
import com.ahorra.feature.report.repository.DailyReportRepository;
import com.ahorra.feature.report.service.ReportQueryService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ReportQueryServiceImpl implements ReportQueryService {

    private static final int DEFAULT_SPAN_DAYS = 30;
    private static final int MAX_SPAN_DAYS = 92;

    private final DailyReportRepository dailyReportRepository;
    private final ZoneId zone;

    public ReportQueryServiceImpl(DailyReportRepository dailyReportRepository,
                                  @Value("${app.timezone:Africa/Lagos}") String timezone) {
        this.dailyReportRepository = dailyReportRepository;
        this.zone = ZoneId.of(timezone);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyReportResponse forDay(Long userId, LocalDate day) {
        DailyReport report = dailyReportRepository.findByUserIdAndReportDate(userId, day)
                .orElseThrow(() -> new EntityNotFoundException("No report for " + day));
        return toResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyReportResponse latest(Long userId) {
        DailyReport report = dailyReportRepository.findFirstByUserIdOrderByReportDateDesc(userId)
                .orElseThrow(() -> new EntityNotFoundException("No reports yet"));
        return toResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyReportSummary> history(Long userId, LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(zone);
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_SPAN_DAYS - 1L);
        if (start.isAfter(end)) {
            return List.of();
        }
        LocalDate earliest = end.minusDays(MAX_SPAN_DAYS - 1L);
        if (start.isBefore(earliest)) {
            start = earliest;
        }
        return dailyReportRepository
                .findByUserIdAndReportDateBetweenOrderByReportDateDesc(userId, start, end)
                .stream()
                .map(r -> new DailyReportSummary(
                        r.getReportDate(), r.getKind(), r.getEntryCount(),
                        r.getDaySpend(), r.getVerdict(), r.getSummaryText()))
                .toList();
    }

    private DailyReportResponse toResponse(DailyReport r) {
        return new DailyReportResponse(
                r.getReportDate(), r.getKind(), r.getSummaryText(),
                r.getPeriodType(), r.getPeriodStart(), r.getPeriodEnd(),
                r.getEntryCount(), r.getDaySpend(), r.getBaseline(), r.getIncomeMax(),
                r.getPeriodSpend(), r.getVariance(), r.getRollingAvgDailySpend(),
                r.getDaysObserved(), r.getVerdict(), r.getInsightText(),
                r.getNarrativeSource(),
                lines(r, CategoryScope.DAY),
                lines(r, CategoryScope.PERIOD));
    }

    private List<CategoryLine> lines(DailyReport report, CategoryScope scope) {
        return report.getCategories().stream()
                .filter(c -> c.getScope() == scope)
                .map(this::toLine)
                .toList();
    }

    private CategoryLine toLine(DailyReportCategory c) {
        return new CategoryLine(c.getCategoryName(), c.getAmount(),
                c.getSharePercent(), c.getAttributedExcess());
    }
}
