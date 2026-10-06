package com.ahorra.feature.appearance.service;

import com.ahorra.feature.user.enums.ThemeMode;

public interface AppearanceService {

    /** The theme the user chose, or null if they never chose one. Kept on the user, so it survives a reinstall. */
    ThemeMode getTheme(Long userId);

    void setTheme(Long userId, ThemeMode theme);
}
