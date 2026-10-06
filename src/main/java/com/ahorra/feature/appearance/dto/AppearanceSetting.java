package com.ahorra.feature.appearance.dto;

import com.ahorra.feature.user.enums.ThemeMode;
import jakarta.validation.constraints.NotNull;

/**
 * The user's saved appearance. On a read, theme is null when they have never chosen one.
 * On a write, theme is required.
 */
public record AppearanceSetting(@NotNull ThemeMode theme) {
}
