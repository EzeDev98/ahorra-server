package com.ahorra.feature.session.service;
import com.ahorra.client.dto.response.LogoutResponse;

public interface LogoutService {
    LogoutResponse logout(String sessionId);
}
