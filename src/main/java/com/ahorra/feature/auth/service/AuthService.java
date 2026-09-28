package com.ahorra.feature.auth.service;

import com.ahorra.client.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(String username, String password);
    LoginResponse refreshAccessToken(String refreshToken);
}
