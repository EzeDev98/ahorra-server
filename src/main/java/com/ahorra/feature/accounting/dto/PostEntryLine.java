package com.ahorra.feature.accounting.dto;

import com.ahorra.feature.accounting.enums.EntryDirection;

import java.math.BigDecimal;

/** One proposed debit or credit line. */
public record PostEntryLine(Long accountId, EntryDirection direction, BigDecimal amount) {
}
