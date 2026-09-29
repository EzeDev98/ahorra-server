package com.ahorra.feature.capture.dto;

import com.ahorra.feature.capture.enums.FlowDirection;
import com.ahorra.feature.capture.enums.PendingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** The pre-filled confirmation card the client shows for one pending entry. */
public record PendingEntryResponse(
        Long id,
        Long rawEntryId,
        PendingStatus status,
        FlowDirection flow,
        LocalDate entryDate,
        String vendor,
        Long categoryId,
        String categoryName,
        Long accountId,
        String accountName,
        List<ParsedItem> items,
        BigDecimal totalAmount
) {
}
