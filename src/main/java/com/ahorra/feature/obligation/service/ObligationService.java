package com.ahorra.feature.obligation.service;

import com.ahorra.feature.obligation.dto.ObligationRequest;
import com.ahorra.feature.obligation.dto.ObligationResponse;
import com.ahorra.feature.obligation.dto.UpdateObligationRequest;

import java.util.List;

public interface ObligationService {

    List<ObligationResponse> list(Long userId);

    ObligationResponse create(Long userId, ObligationRequest request);

    ObligationResponse update(Long userId, Long id, UpdateObligationRequest request);

    void delete(Long userId, Long id);
}
