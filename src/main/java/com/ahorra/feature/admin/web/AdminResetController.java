package com.ahorra.feature.admin.web;

import com.ahorra.common.response.BaseResponse;
import com.ahorra.feature.admin.dto.WipeRequest;
import com.ahorra.feature.admin.security.ResetAuthorizer;
import com.ahorra.feature.admin.service.DatabaseResetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Danger-zone endpoint for wiping the whole dataset in a test environment -
 * both SyncPoint's own database and, via {@link DatabaseResetService}'s
 * downstream call, every internal-auth-server record scoped to this app.
 * <p>
 * Deliberately thin: it authorizes via {@link ResetAuthorizer}, delegates
 * the work to {@link DatabaseResetService}, and returns the combined
 * report. A rejected request throws from the guard and is shaped by
 * {@code GlobalExceptionHandler}. The path is opened in
 * {@code WebSecurityConfig} (no login required) so it can be triggered
 * with a single request — the secret is the guard.
 */
@Slf4j
@RestController
@RequestMapping("${app.title}")
@RequiredArgsConstructor
@Tag(name = "Admin Reset API", description = "Destructive maintenance — wipes all data in test environments")
public class AdminResetController {

    private final ResetAuthorizer resetAuthorizer;
    private final DatabaseResetService databaseResetService;

    @PostMapping("/admin/reset/wipe")
    public ResponseEntity<BaseResponse> wipeEverything(
            @RequestHeader(value = "X-Reset-Secret", required = false) String providedSecret,
            @RequestBody(required = false) WipeRequest request) {

        String confirmation = request != null ? request.getConfirmation() : null;

        resetAuthorizer.authorize(providedSecret, confirmation);

        log.warn("[Reset] AUTHORIZED wipe starting.");

        DatabaseResetService.ResetResult result = databaseResetService.wipeDatabase();
        Map<String, Long> internalAuthCounts = databaseResetService.wipeInternalAuthTenantData();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deletedCounts", result.deletedCounts());
        data.put("internalAuthDeletedCounts", internalAuthCounts);

        return ResponseEntity.ok(new BaseResponse(HttpStatus.OK.value(), "All data wiped. Environment is clean.", data));
    }
}
