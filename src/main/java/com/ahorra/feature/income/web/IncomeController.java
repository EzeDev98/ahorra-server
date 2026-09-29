package com.ahorra.feature.income.web;

import com.ahorra.feature.income.dto.IncomeRequest;
import com.ahorra.feature.income.dto.IncomeResponse;
import com.ahorra.feature.income.service.IncomeService;
import com.ahorra.feature.user.service.impl.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("${app.title}")
@Tag(name = "Income API", description = "Resource for the user's stated income range and its history")
public class IncomeController {

    private final IncomeService incomeService;
    private final CurrentUserService currentUserService;

    @GetMapping("/income")
    public ResponseEntity<IncomeResponse> getCurrent() {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(incomeService.getCurrent(userId));
    }

    @PutMapping("/income")
    public ResponseEntity<IncomeResponse> setIncome(@RequestBody IncomeRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(incomeService.setIncome(userId, request));
    }

    @GetMapping("/income/history")
    public ResponseEntity<List<IncomeResponse>> getHistory() {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(incomeService.getHistory(userId));
    }
}
