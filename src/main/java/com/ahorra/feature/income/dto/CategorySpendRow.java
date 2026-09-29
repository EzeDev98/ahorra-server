package com.ahorra.feature.income.dto;

import java.math.BigDecimal;

/** Query result: confirmed spend for one category (null id means uncategorized). */
public record CategorySpendRow(Long categoryId, BigDecimal amount) {
}
