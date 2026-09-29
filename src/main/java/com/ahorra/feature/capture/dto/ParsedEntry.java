package com.ahorra.feature.capture.dto;

import java.util.List;

/**
 * Raw, UNVALIDATED output of the LLM parser. Everything here is untrusted
 * text until ParsedEntryValidator has checked it.
 */
public record ParsedEntry(
        String flow,
        String entryDate,
        String vendor,
        String categoryName,
        String accountName,
        List<ParsedItem> items
) {
}
