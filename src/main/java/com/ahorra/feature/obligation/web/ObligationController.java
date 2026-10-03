package com.ahorra.feature.obligation.web;

import com.ahorra.feature.obligation.dto.ObligationRequest;
import com.ahorra.feature.obligation.dto.ObligationResponse;
import com.ahorra.feature.obligation.dto.UpdateObligationRequest;
import com.ahorra.feature.obligation.service.ObligationService;
import com.ahorra.feature.user.service.impl.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("${app.title}")
@Tag(name = "Obligation API", description = "Resource for the user's fixed obligations (rent, data, electricity and similar)")
public class ObligationController {

    private final ObligationService obligationService;
    private final CurrentUserService currentUserService;

    @GetMapping("/obligations")
    public ResponseEntity<List<ObligationResponse>> list() {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(obligationService.list(userId));
    }

    @PostMapping("/obligations")
    public ResponseEntity<ObligationResponse> create(@Valid @RequestBody ObligationRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(obligationService.create(userId, request));
    }

    @PutMapping("/obligations/{id}")
    public ResponseEntity<ObligationResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateObligationRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(obligationService.update(userId, id, request));
    }

    @DeleteMapping("/obligations/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUser().getId();
        obligationService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
