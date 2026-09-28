package com.ahorra.feature.auth.service.impl;

import com.ahorra.client.dto.request.LoginRequest;
import com.ahorra.client.dto.request.RefreshTokenRequest;
import com.ahorra.client.dto.response.InternalAuthApiResponse;
import com.ahorra.client.dto.response.InternalAuthLoginResponse;
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
    public LoginResponse login(String username, String password) {
        InternalAuthApiResponse<InternalAuthLoginResponse> response = authClient.login(new LoginRequest(username, password));
        InternalAuthLoginResponse data = response.getData();
        return new LoginResponse(
                data.getUserId(),
                data.getUsername(),
                data.getMessage(),
                data.isMfaRequired(),
                data.getAccessToken(),
                data.getRefreshToken(),
                data.getExpiresIn(),
                data.getRefreshExpiresIn()
        );
    }

    @Override
    public LoginResponse refreshAccessToken(String refreshToken) {
        InternalAuthApiResponse<LoginResponse> loginResponse = authClient.refreshToken(new RefreshTokenRequest(refreshToken));
        return loginResponse.getData();
    }
}
