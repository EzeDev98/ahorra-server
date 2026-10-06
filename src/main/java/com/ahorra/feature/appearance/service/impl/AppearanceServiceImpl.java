package com.ahorra.feature.appearance.service.impl;

import com.ahorra.feature.appearance.service.AppearanceService;
import com.ahorra.feature.user.enums.ThemeMode;
import com.ahorra.feature.user.model.User;
import com.ahorra.feature.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppearanceServiceImpl implements AppearanceService {

    private final UserRepository userRepository;

    @Override
    public ThemeMode getTheme(Long userId) {
        return userRepository.findById(userId).map(User::getTheme).orElse(null);
    }

    @Override
    @Transactional
    public void setTheme(Long userId, ThemeMode theme) {
        userRepository.findById(userId).ifPresent(user -> user.setTheme(theme));
    }
}
