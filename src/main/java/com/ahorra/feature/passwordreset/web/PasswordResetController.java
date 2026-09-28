package com.ahorra.feature.passwordreset.web;

import com.ahorra.client.dto.request.CompletePasswordResetRequest;
import com.ahorra.client.dto.request.PasswordResetRequest;
import com.ahorra.client.dto.request.VerifyOtpRequest;
import com.ahorra.client.dto.response.PasswordResetResponse;
import com.ahorra.client.dto.response.VerifyOtpResponse;
import com.ahorra.common.util.RateLimiterService;
import com.ahorra.feature.passwordreset.service.PasswordResetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import static com.ahorra.common.constants.GeneralConstants.MAX_RESET_PASSWORD_REQUEST;
import static com.ahorra.common.constants.GeneralConstants.RESET_PASSWORD_RATE_LIMIT_DURATION;

@RestController
@RequestMapping("${app.title}/password-reset")
@Tag(name = "Password Reset API", description = "Phone-number OTP based password reset flow")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final RateLimiterService rateLimiterService;

    public PasswordResetController(PasswordResetService passwordResetService, RateLimiterService rateLimiterService) {
        this.passwordResetService = passwordResetService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/request")
    public ResponseEntity<PasswordResetResponse> requestOtp(@RequestBody PasswordResetRequest request) {
        rateLimiterService.checkRateLimit("ratelimit:password-reset-otp:phone:" + request.getPhoneNumber(), MAX_RESET_PASSWORD_REQUEST, Duration.ofHours(RESET_PASSWORD_RATE_LIMIT_DURATION));
        PasswordResetResponse passwordResetResponse = passwordResetService.requestOtp(request);
        return ResponseEntity.ok(passwordResetResponse);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse verifyOtpResponse = passwordResetService.verifyOtp(request);
        return ResponseEntity.ok(verifyOtpResponse);
    }

    @PostMapping("/complete")
    public ResponseEntity<PasswordResetResponse> completeReset(@RequestBody CompletePasswordResetRequest request) {
        PasswordResetResponse passwordResetResponse = passwordResetService.completeReset(request);
        return ResponseEntity.ok(passwordResetResponse);
    }
}