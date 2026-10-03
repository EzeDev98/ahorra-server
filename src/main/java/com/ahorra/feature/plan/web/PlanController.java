package com.ahorra.feature.plan.web;

import com.ahorra.feature.plan.dto.PlanResponse;
import com.ahorra.feature.plan.service.PlanService;
import com.ahorra.feature.user.service.impl.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("${app.title}")
@Tag(name = "Plan API", description = "Resource for the 50/30/20 spending plan of the current income period")
public class PlanController {

    private final PlanService planService;
    private final CurrentUserService currentUserService;

    @GetMapping("/plan")
    public ResponseEntity<PlanResponse> current() {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(planService.currentPlan(userId));
    }
}
