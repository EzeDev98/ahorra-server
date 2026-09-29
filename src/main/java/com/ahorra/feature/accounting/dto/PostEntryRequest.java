package com.ahorra.feature.accounting.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * A fully-formed proposal to post to the ledger. Built by the backend (from a
 * confirmed pending entry), never taken straight from the AI or the client.
 */
public record PostEntryRequest(
        Long userId,
        Long categoryId,
        LocalDate entryDate,
        String description,
        List<PostEntryLine> lines
) {
}
