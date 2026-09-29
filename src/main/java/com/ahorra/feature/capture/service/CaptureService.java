package com.ahorra.feature.capture.service;

import com.ahorra.feature.capture.dto.CaptureRequest;
import com.ahorra.feature.capture.dto.PendingEntryResponse;

public interface CaptureService {

    /**
     * Stores the raw input, parses it, and saves the result as a PENDING
     * entry awaiting confirmation. Does NOT post anything to the ledger.
     * Retrying with the same clientRequestId returns the existing entry.
     */
    PendingEntryResponse capture(Long userId, CaptureRequest request);
}
