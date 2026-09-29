package com.ahorra.feature.admin.web;

import com.ahorra.feature.behavior.service.BehaviorAnalysisService;
import com.ahorra.feature.day.service.DayCloseService;
import com.ahorra.feature.report.service.DailyReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin-only maintenance. Re-runs, for one user, the same catch-up work the
 * nightly job does: close any days that are ready, backfill missing reports,
 * and refresh baselines and habits. Every step is idempotent.
 */
@RestController
@RequestMapping("${app.title}")
@Tag(name = "Admin Maintenance API", description = "Admin-only operations for repairing a user's derived data")
public class AdminMaintenanceController {

    private final DayCloseService dayCloseService;
    private final DailyReportService dailyReportService;
    private final BehaviorAnalysisService behaviorAnalysisService;
    private final ZoneId zone;

    public AdminMaintenanceController(DayCloseService dayCloseService,
                                      DailyReportService dailyReportService,
                                      BehaviorAnalysisService behaviorAnalysisService,
                                      @Value("${app.timezone:Africa/Lagos}") String timezone) {
        this.dayCloseService = dayCloseService;
        this.dailyReportService = dailyReportService;
        this.behaviorAnalysisService = behaviorAnalysisService;
        this.zone = ZoneId.of(timezone);
    }

    @PostMapping("/admin/maintenance/users/{userId}/refresh")
    @PreAuthorize("@rolesEvaluator.isAdmin(authentication)")
    public ResponseEntity<Map<String, Object>> refresh(@PathVariable Long userId) {
        LocalDate through = LocalDate.now(zone).minusDays(1);
        dayCloseService.catchUp(userId, through);
        dailyReportService.generateMissing(userId);
        behaviorAnalysisService.refresh(userId, through);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("through", through);
        result.put("status", "refreshed");
        return ResponseEntity.ok(result);
    }
}
