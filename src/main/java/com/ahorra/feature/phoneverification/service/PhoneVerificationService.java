package com.ahorra.feature.phoneverification.service;

import com.ahorra.client.dto.request.ResendPhoneVerificationRequest;
import com.ahorra.client.dto.request.VerifyOtpRequest;
import com.ahorra.client.dto.response.ActivationResponse;

public interface PhoneVerificationService {
    ActivationResponse verifyPhone(VerifyOtpRequest request);
    ActivationResponse resendVerification(ResendPhoneVerificationRequest request);
}
