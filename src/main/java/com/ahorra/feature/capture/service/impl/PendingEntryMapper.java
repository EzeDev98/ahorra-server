package com.ahorra.feature.capture.service.impl;

import com.ahorra.feature.accounting.model.Account;
import com.ahorra.feature.accounting.model.Category;
import com.ahorra.feature.accounting.repository.AccountRepository;
import com.ahorra.feature.accounting.repository.CategoryRepository;
import com.ahorra.feature.capture.dto.ParsedItem;
import com.ahorra.feature.capture.dto.PendingEntryResponse;
import com.ahorra.feature.capture.model.PendingEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Builds confirmation-card responses, looking category/account names up in one batch per list. */
@Component
@RequiredArgsConstructor
public class PendingEntryMapper {

    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    public PendingEntryResponse toResponse(PendingEntry entry) {
        return toResponses(List.of(entry)).get(0);
    }

    public List<PendingEntryResponse> toResponses(List<PendingEntry> entries) {
        Set<Long> categoryIds = entries.stream().map(PendingEntry::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> accountIds = entries.stream().map(PendingEntry::getAccountId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> categoryNames = categoryIds.isEmpty() ? Map.of()
                : categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<Long, String> accountNames = accountIds.isEmpty() ? Map.of()
                : accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, Account::getName));

        return entries.stream().map(p -> new PendingEntryResponse(
                p.getId(), p.getRawEntryId(), p.getStatus(), p.getFlow(), p.getEntryDate(), p.getVendor(),
                p.getCategoryId(), p.getCategoryId() == null ? null : categoryNames.get(p.getCategoryId()),
                p.getAccountId(), p.getAccountId() == null ? null : accountNames.get(p.getAccountId()),
                p.getItems().stream().map(i -> new ParsedItem(i.getDescription(), i.getAmount())).toList(),
                p.getTotalAmount())).toList();
    }
}
