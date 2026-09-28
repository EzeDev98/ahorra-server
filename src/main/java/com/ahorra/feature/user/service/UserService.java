package com.ahorra.feature.user.service;

import com.ahorra.feature.auth.dto.request.RegistrationRequest;
import com.ahorra.feature.auth.dto.response.UserRegistrationResponse;

public interface UserService {
    UserRegistrationResponse registerUser(RegistrationRequest request);
}
