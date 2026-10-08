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

    /**
     * Throws a pending entry away without posting anything. The row is kept (status
     * DISCARDED) so it can be restored. Discarding an already discarded entry returns it
     * unchanged; a confirmed entry cannot be discarded.
     */
    PendingEntryResponse discard(Long userId, Long pendingEntryId);

    /**
     * Undoes a discard. If the entry's day has closed in the meantime it comes back dated
     * today, so it can never be left stranded on a closed day. Restoring an entry that is
     * already pending returns it unchanged.
     */
    PendingEntryResponse restore(Long userId, Long pendingEntryId);
}
