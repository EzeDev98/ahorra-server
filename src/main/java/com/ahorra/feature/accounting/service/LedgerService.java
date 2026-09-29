package com.ahorra.feature.accounting.service;

import com.ahorra.feature.accounting.dto.PostEntryRequest;
import com.ahorra.feature.accounting.model.LedgerEntry;

public interface LedgerService {

    /**
     * Validates the proposal against the double-entry rules and posts it.
     * Throws BadRequestException if any rule is violated; nothing is written in that case.
     */
    LedgerEntry postEntry(PostEntryRequest request);
}
