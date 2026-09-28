package com.ahorra.feature.phoneverification.web;

import com.ahorra.client.dto.request.ResendPhoneVerificationRequest;
import com.ahorra.client.dto.request.VerifyOtpRequest;
import com.ahorra.client.dto.response.ActivationResponse;
import com.ahorra.feature.phoneverification.service.PhoneVerificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service phone verification via SMS OTP. Mapped onto the
 * "/phone-number/verify" and "/phone-number/resend-verification" paths
 * WebSecurityConfig already permits publicly - a newly registered or
 * phone-unverified user hitting this flow by definition has no bearer
 * token yet, so both endpoints must stay open.
 */
@RestController
@RequestMapping("${app.title}/phone-number")
@Tag(name = "Phone Verification API", description = "Account phone-number verification via SMS OTP")
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    public PhoneVerificationController(PhoneVerificationService phoneVerificationService) {
        this.phoneVerificationService = phoneVerificationService;
    }

    @PostMapping("/verify")
    public ResponseEntity<ActivationResponse> verify(@RequestBody VerifyOtpRequest request) {
        ActivationResponse response = phoneVerificationService.verifyPhone(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ActivationResponse> resendVerification(@RequestBody ResendPhoneVerificationRequest request) {
        ActivationResponse response = phoneVerificationService.resendVerification(request);
        return ResponseEntity.ok(response);
    }
}
