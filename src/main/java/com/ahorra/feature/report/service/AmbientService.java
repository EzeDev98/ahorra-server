package com.ahorra.feature.report.service;

import com.ahorra.feature.report.dto.AmbientResponse;

public interface AmbientService {

    /** Today's running total for the user. Throws DayGatedException while capture is gated. */
    AmbientResponse today(Long userId);
}
