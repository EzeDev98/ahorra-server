package com.ahorra.feature.capture.dto;

import com.ahorra.feature.capture.enums.FlowDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A validated proposal built from the user's raw input. Category and account
 * ids are only set when the AI's suggested name matched one of the user's own
 * records; otherwise they are null and the name is null too.
 */
public record ParsedEntryProposal(
        Long rawEntryId,
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
