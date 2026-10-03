package com.ahorra.feature.push.web;

import com.ahorra.feature.push.dto.PushSubscriptionRequest;
import com.ahorra.feature.push.service.PushSubscriptionService;
import com.ahorra.feature.user.service.impl.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("${app.title}")
@Tag(name = "Push API", description = "Resource for the devices that receive the daily nudge")
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;
    private final CurrentUserService currentUserService;

    @PostMapping("/push/subscriptions")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody PushSubscriptionRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        pushSubscriptionService.subscribe(userId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/push/subscriptions")
    public ResponseEntity<Void> unsubscribe(@RequestParam String endpoint) {
        Long userId = currentUserService.getCurrentUser().getId();
        pushSubscriptionService.unsubscribe(userId, endpoint);
        return ResponseEntity.noContent().build();
    }
}
