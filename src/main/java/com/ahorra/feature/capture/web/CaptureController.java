package com.ahorra.feature.capture.web;

import com.ahorra.feature.capture.dto.CaptureRequest;
import com.ahorra.feature.capture.dto.PendingEntryResponse;
import com.ahorra.feature.capture.service.CaptureService;
import com.ahorra.feature.user.service.impl.CurrentUserService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("${app.title}")
@Tag(name = "Capture API", description = "Resource for submitting spoken or typed entries for parsing")
public class CaptureController {

    private final CaptureService captureService;
    private final CurrentUserService currentUserService;

    @PostMapping("/entries/capture")
    public ResponseEntity<PendingEntryResponse> capture(@Valid @RequestBody CaptureRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(captureService.capture(userId, request));
    }
}
