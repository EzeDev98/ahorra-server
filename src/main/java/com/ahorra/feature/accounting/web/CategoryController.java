package com.ahorra.feature.accounting.web;

import com.ahorra.feature.accounting.dto.LookupItem;
import com.ahorra.feature.accounting.service.impl.LookupCacheService;
import com.ahorra.feature.user.service.impl.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("${app.title}")
@Tag(name = "Category API", description = "Read-only list of the signed-in user's active categories")
public class CategoryController {

    private final LookupCacheService lookupCacheService;
    private final CurrentUserService currentUserService;

    /** The user's active categories as { id, name }, sorted by name, for the client's category picker. */
    @GetMapping("/categories")
    public ResponseEntity<List<LookupItem>> list() {
        Long userId = currentUserService.getCurrentUser().getId();
        List<LookupItem> categories = lookupCacheService.categories(userId).stream()
                .sorted(Comparator.comparing(LookupItem::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return ResponseEntity.ok(categories);
    }
}
