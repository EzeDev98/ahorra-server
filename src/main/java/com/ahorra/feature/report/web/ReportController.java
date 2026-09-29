package com.ahorra.feature.report.web;

import com.ahorra.feature.report.dto.DailyReportResponse;
import com.ahorra.feature.report.dto.DailyReportSummary;
import com.ahorra.feature.report.service.ReportQueryService;
import com.ahorra.feature.user.service.impl.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("${app.title}")
@RequiredArgsConstructor
public class ReportController {

    private final ReportQueryService reportQueryService;
    private final CurrentUserService currentUserService;

    /** Newest-first summaries. Optional ?from=YYYY-MM-DD&to=YYYY-MM-DD, default last 30 days. */
    @GetMapping("/report/daily")
    public ResponseEntity<List<DailyReportSummary>> history(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(reportQueryService.history(userId, from, to));
    }

    @GetMapping("/report/daily/latest")
    public ResponseEntity<DailyReportResponse> latest() {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(reportQueryService.latest(userId));
    }

    @GetMapping("/report/daily/{date}")
    public ResponseEntity<DailyReportResponse> forDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(reportQueryService.forDay(userId, date));
    }
}
