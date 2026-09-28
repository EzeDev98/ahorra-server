package com.ahorra.feature.auth.web;

import com.ahorra.client.dto.response.LoginResponse;
import com.ahorra.feature.auth.service.AuthService;
import com.ahorra.feature.session.service.SessionManagementService;
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

    public AuthController(AuthService authService, SessionManagementService sessionManagementService,
                          JwtDecoder jwtDecoder, CurrentUserService currentUserService) {
        this.authService = authService;
        this.sessionManagementService = sessionManagementService;
        this.jwtDecoder = jwtDecoder;
        this.currentUserService = currentUserService;
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