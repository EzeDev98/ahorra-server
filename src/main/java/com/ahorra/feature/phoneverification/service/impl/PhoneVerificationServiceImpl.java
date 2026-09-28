package com.ahorra.feature.phoneverification.service.impl;

import com.ahorra.client.dto.request.ResendPhoneVerificationRequest;
import com.ahorra.client.dto.request.VerifyOtpRequest;
import com.ahorra.client.dto.response.ActivationResponse;
import com.ahorra.client.dto.response.InternalAuthApiResponse;
import com.ahorra.client.service.InternalAuthClient;
import com.ahorra.feature.phoneverification.service.PhoneVerificationService;
import org.springframework.stereotype.Service;

@Service
public class PhoneVerificationServiceImpl implements PhoneVerificationService {

    private final InternalAuthClient authClient;

    public PhoneVerificationServiceImpl(InternalAuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    public ActivationResponse verifyPhone(VerifyOtpRequest request) {
        InternalAuthApiResponse<ActivationResponse> response = authClient.verifyPhone(request);
        return response.getData();
    }

    @Override
    public ActivationResponse resendVerification(ResendPhoneVerificationRequest request) {
        InternalAuthApiResponse<ActivationResponse> response = authClient.resendPhoneVerification(request);
        return response.getData();
    }
}
