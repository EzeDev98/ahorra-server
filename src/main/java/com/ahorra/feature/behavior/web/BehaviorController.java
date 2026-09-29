package com.ahorra.feature.behavior.web;

import com.ahorra.feature.behavior.dto.BehaviorOverview;
import com.ahorra.feature.behavior.service.BehaviorOverviewService;
import com.ahorra.feature.user.service.impl.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.title}")
@RequiredArgsConstructor
public class BehaviorController {

    private final BehaviorOverviewService behaviorOverviewService;
    private final CurrentUserService currentUserService;

    @GetMapping("/behavior/overview")
    public ResponseEntity<BehaviorOverview> overview() {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(behaviorOverviewService.overview(userId));
    }
}
