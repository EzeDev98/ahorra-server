package com.ahorra.feature.day.web;

import com.ahorra.feature.day.dto.DayStatusResponse;
import com.ahorra.feature.day.service.DayGateService;
import com.ahorra.feature.user.service.impl.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("${app.title}")
@Tag(name = "Day API", description = "Resource for the day-close gate and forced resolution")
public class DayController {

    private final DayGateService dayGateService;
    private final CurrentUserService currentUserService;

    @GetMapping("/day/status")
    public ResponseEntity<DayStatusResponse> status() {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(dayGateService.status(userId));
    }
}
