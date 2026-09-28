package com.ahorra.feature.session.service.impl;

import com.ahorra.client.dto.response.InternalAuthApiResponse;
import com.ahorra.client.dto.response.LogoutResponse;
import com.ahorra.client.service.InternalAuthClient;
import com.ahorra.common.exception.BadRequestException;
import com.ahorra.feature.session.model.UserSession;
import com.ahorra.feature.session.repository.UserSessionRepository;
import com.ahorra.feature.session.service.LogoutService;
import com.ahorra.feature.session.service.SessionManagementService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogoutServiceImpl implements LogoutService {

    private final UserSessionRepository userSessionRepo;
    private final SessionManagementService sessionManagementService;
    private final InternalAuthClient authClient;

    public LogoutServiceImpl(UserSessionRepository userSessionRepo, SessionManagementService sessionManagementService, InternalAuthClient authClient) {
        this.userSessionRepo = userSessionRepo;
        this.sessionManagementService = sessionManagementService;
        this.authClient = authClient;
    }

    @Transactional
    @Override
    public LogoutResponse logout(String token) {

        if (token == null || token.trim().isEmpty()) {
            log.warn("Attempted logout with null or empty token");
            throw new BadRequestException("Invalid token");
        }

        try {

            UserSession session = userSessionRepo.findByToken(token)
                    .orElseThrow(() -> new BadRequestException("No active session found"));

            InternalAuthApiResponse<LogoutResponse> logout = authClient.logout(token);
            if (logout == null || !logout.isSuccess()) {
                log.warn("Upstream logout failed for user: {} - leaving local session intact", session.getPhoneNumber());
                return logout != null ? logout.getData() : new LogoutResponse("Logout failed. Please try again.", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            sessionManagementService.invalidateSession(token);
            log.info("Session deleted for user: {}", session.getPhoneNumber());

            return logout.getData();
        } catch (BadRequestException ex) {
            log.warn("Logout failed: {}", ex.getMessage());
            return new LogoutResponse(ex.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception ex) {
            log.error("Error during logout: {}", ex.getMessage());
            return new LogoutResponse("Logout failed. Please try again.", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}