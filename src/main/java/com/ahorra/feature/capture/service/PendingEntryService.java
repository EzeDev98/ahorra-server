package com.ahorra.feature.capture.service;

import com.ahorra.feature.capture.dto.ConfirmRequest;
import com.ahorra.feature.capture.dto.PendingEntryResponse;

import java.util.List;

public interface PendingEntryService {

    /** The user's unconfirmed entries, oldest day first. */
    List<PendingEntryResponse> listPending(Long userId);

    /**
     * Confirms a pending entry, optionally applying edits first, and posts it
     * to the ledger. Confirming an already-confirmed entry with no edits
     * returns it unchanged.
     */
    PendingEntryResponse confirm(Long userId, Long pendingEntryId, ConfirmRequest edits);
}
