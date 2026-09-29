package com.ahorra.feature.capture.dto;

import java.math.BigDecimal;

/** One line item extracted from the user's input, e.g. "egg" 300.00. */
public record ParsedItem(String description, BigDecimal amount) {
}
