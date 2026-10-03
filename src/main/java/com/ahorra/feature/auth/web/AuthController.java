package com.ahorra.feature.auth.web;

import com.ahorra.feature.auth.dto.request.LoginRequest;

import com.ahorra.client.dto.response.LoginResponse;
import com.ahorra.feature.auth.service.AuthService;
import com.ahorra.feature.pin.service.PinService;
import com.ahorra.feature.session.service.SessionManagementService;
import com.ahorra.feature.user.model.User;
import com.ahorra.feature.user.service.impl.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("${app.title}")
@Tag(name = "Auth API", description = "Resource for Internal Auth & Token validation and generation")
public class AuthController {

    private final AuthService authService;
    private final JwtDecoder jwtDecoder;
    private final CurrentUserService currentUserService;
    private final SessionManagementService sessionManagementService;
    private final PinService pinService;

    public AuthController(AuthService authService, SessionManagementService sessionManagementService,
                          PinService pinService,
                          JwtDecoder jwtDecoder, CurrentUserService currentUserService) {
        this.authService = authService;
        this.sessionManagementService = sessionManagementService;
        this.pinService = pinService;
        this.jwtDecoder = jwtDecoder;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username and password are required"));
        }

        LoginResponse loginResponse = authService.login(username, password);

        if (loginResponse.isMfaRequired()) {
            // No access token was issued yet - internal-auth-server is
            // waiting on a follow-up OTP verification call this endpoint
            // does not implement. Nothing to open a local session with.
            return ResponseEntity.ok(loginResponse);
        }

        try {
            Jwt jwt = jwtDecoder.decode(loginResponse.getAccessToken());
            User user = currentUserService.resolveUser(jwt);
            sessionManagementService.createSession(user.getId(), user.getPhoneNumber(), loginResponse.getAccessToken());
            pinService.markFreshLogin(user.getId());
        } catch (Exception ex) {
            log.warn("Could not resolve local user for new login; local session not opened", ex);
        }

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/auth/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Refresh token is required"));
        }

        LoginResponse tokenResponse = authService.refreshAccessToken(refreshToken);

        try {
            Jwt jwt = jwtDecoder.decode(tokenResponse.getAccessToken());
            String ownPhoneNumber = currentUserService.resolveUser(jwt).getPhoneNumber();
            sessionManagementService.updateSessionOnRefresh(ownPhoneNumber, tokenResponse.getAccessToken());
        } catch (Exception ex) {
            log.warn("Could not resolve local user for refreshed token; session TTL not reset", ex);
        }

        return ResponseEntity.ok(tokenResponse);
    }
}
