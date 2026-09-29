package com.ahorra.feature.capture.service.impl;

import com.ahorra.common.exception.BadRequestException;
import com.ahorra.common.money.MoneyUtils;
import com.ahorra.feature.accounting.dto.LookupItem;
import com.ahorra.feature.capture.dto.ParsedEntry;
import com.ahorra.feature.capture.dto.ParsedEntryProposal;
import com.ahorra.feature.capture.dto.ParsedItem;
import com.ahorra.feature.capture.enums.FlowDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.ahorra.common.money.MoneyConfig.MONETARY_SCALE;

/**
 * Deterministic checks on the AI's output. Nothing the model says is trusted:
 * amounts, dates, flow and record names are all verified here in code.
 */
@Component
public class ParsedEntryValidator {

    private static final int MAX_ITEMS = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 255;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");

    public ParsedEntryProposal validate(Long rawEntryId, ParsedEntry parsed, LocalDate today,
                                        List<LookupItem> accounts, List<LookupItem> categories) {
        FlowDirection flow = parseFlow(parsed.flow());
        List<ParsedItem> items = validateItems(parsed.items());
        BigDecimal total = items.stream().map(ParsedItem::amount).reduce(BigDecimal.ZERO, MoneyUtils::add);

        LookupItem category = parsed.categoryName() == null ? null : categories.stream()
                .filter(c -> c.name().trim().equalsIgnoreCase(parsed.categoryName()))
                .findFirst().orElse(null);
        LookupItem account = parsed.accountName() == null ? null : accounts.stream()
                .filter(a -> a.name().trim().equalsIgnoreCase(parsed.accountName()))
                .findFirst().orElse(null);

        return new ParsedEntryProposal(
                rawEntryId,
                flow,
                resolveDate(parsed.entryDate(), today),
                parsed.vendor(),
                category == null ? null : category.id(),
                category == null ? null : category.name(),
                account == null ? null : account.id(),
                account == null ? null : account.name(),
                items,
                total);
    }

    private FlowDirection parseFlow(String flow) {
        try {
            return FlowDirection.valueOf(flow == null ? "" : flow.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Could not tell whether this was money spent or received");
        }
    }

    public List<ParsedItem> validateItems(List<ParsedItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("No amounts were found in that entry");
        }
        if (items.size() > MAX_ITEMS) {
            throw new BadRequestException("Too many items in one entry");
        }
        List<ParsedItem> clean = new ArrayList<>();
        for (ParsedItem item : items) {
            String description = item.description();
            BigDecimal amount = item.amount();
            if (description == null || description.isBlank()) {
                throw new BadRequestException("Every item needs a description");
            }
            if (amount == null || amount.stripTrailingZeros().scale() > MONETARY_SCALE) {
                throw new BadRequestException("Could not read an amount for: " + description);
            }
            BigDecimal normalized = MoneyUtils.normalize(amount);
            if (!MoneyUtils.isPositive(normalized) || normalized.compareTo(MAX_AMOUNT) >= 0) {
                throw new BadRequestException("Invalid amount for: " + description);
            }
            String trimmed = description.trim();
            if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
                trimmed = trimmed.substring(0, MAX_DESCRIPTION_LENGTH);
            }
            clean.add(new ParsedItem(trimmed, normalized));
        }
        return clean;
    }

    /** Missing, unreadable, or future dates fall back to today. */
    private LocalDate resolveDate(String value, LocalDate today) {
        if (value == null) {
            return today;
        }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            return date.isAfter(today) ? today : date;
        } catch (DateTimeParseException ex) {
            return today;
        }
    }
}
