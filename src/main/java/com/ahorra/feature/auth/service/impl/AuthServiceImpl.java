package com.ahorra.feature.auth.service.impl;

import com.ahorra.client.dto.request.RefreshTokenRequest;
import com.ahorra.client.dto.response.InternalAuthApiResponse;
import com.ahorra.client.dto.response.LoginResponse;
import com.ahorra.client.service.InternalAuthClient;
import com.ahorra.feature.auth.service.AuthService;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceImpl implements AuthService {

    private final InternalAuthClient authClient;

    public AuthServiceImpl(InternalAuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    public LoginResponse refreshAccessToken(String refreshToken) {
        InternalAuthApiResponse<LoginResponse> loginResponse = authClient.refreshToken(new RefreshTokenRequest(refreshToken));
        return loginResponse.getData();
    }
}
