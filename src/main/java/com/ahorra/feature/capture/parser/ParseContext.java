package com.ahorra.feature.capture.parser;

import java.time.LocalDate;
import java.util.List;

/** Everything the parser is allowed to see when interpreting one entry. */
public record ParseContext(
        String rawText,
        LocalDate today,
        List<String> accountNames,
        List<String> categoryNames
) {
}
