package com.ahorra.feature.capture.parser;

import com.ahorra.feature.capture.dto.ParsedEntry;

/**
 * Turns a rough natural-language entry into structured data. Implementations
 * only extract; they never decide financial correctness and never touch the
 * database.
 */
public interface EntryParser {
    ParsedEntry parse(ParseContext context);
}
