package com.ahorra.feature.capture.dto;

import com.ahorra.feature.capture.enums.FlowDirection;

import java.time.LocalDate;
import java.util.List;

/**
 * Optional edits sent with a confirm. A null field means "leave as is".
 * Sending no body at all confirms the entry exactly as parsed.
 */
public record ConfirmRequest(
        FlowDirection flow,
        LocalDate entryDate,
        String vendor,
        Long categoryId,
        Long accountId,
        List<ParsedItem> items
) {
    public boolean hasEdits() {
        return flow != null || entryDate != null || vendor != null
                || categoryId != null || accountId != null || items != null;
    }
}
