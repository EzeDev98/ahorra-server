package com.ahorra.feature.capture.web;

import com.ahorra.feature.capture.dto.ConfirmRequest;
import com.ahorra.feature.capture.dto.PendingEntryResponse;
import com.ahorra.feature.capture.service.PendingEntryService;
import com.ahorra.feature.user.service.impl.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("${app.title}")
@Tag(name = "Pending Entries API", description = "Resource for reviewing and confirming parsed entries")
public class PendingEntryController {

    private final PendingEntryService pendingEntryService;
    private final CurrentUserService currentUserService;

    @GetMapping("/entries/pending")
    public ResponseEntity<List<PendingEntryResponse>> listPending() {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(pendingEntryService.listPending(userId));
    }

    @PostMapping("/entries/pending/{id}/confirm")
    public ResponseEntity<PendingEntryResponse> confirm(@PathVariable Long id,
                                                        @RequestBody(required = false) ConfirmRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(pendingEntryService.confirm(userId, id, request));
    }
}
