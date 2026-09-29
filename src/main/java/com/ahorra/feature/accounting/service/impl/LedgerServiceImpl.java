package com.ahorra.feature.accounting.service.impl;

import com.ahorra.common.exception.BadRequestException;
import com.ahorra.feature.accounting.dto.PostEntryLine;
import com.ahorra.feature.accounting.dto.PostEntryRequest;
import com.ahorra.feature.accounting.enums.EntryDirection;
import com.ahorra.feature.accounting.model.Account;
import com.ahorra.feature.accounting.model.LedgerEntry;
import com.ahorra.feature.accounting.model.TransactionEntry;
import com.ahorra.feature.accounting.repository.AccountRepository;
import com.ahorra.feature.accounting.repository.CategoryRepository;
import com.ahorra.feature.accounting.repository.LedgerEntryRepository;
import com.ahorra.feature.accounting.service.LedgerService;
import com.ahorra.common.money.MoneyUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ahorra.common.money.MoneyConfig.MONETARY_SCALE;

@Slf4j
@Service
public class LedgerServiceImpl implements LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public LedgerServiceImpl(LedgerEntryRepository ledgerEntryRepository,
                             AccountRepository accountRepository,
                             CategoryRepository categoryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public LedgerEntry postEntry(PostEntryRequest request) {
        validateHeader(request);
        List<PostEntryLine> lines = request.lines();
        validateLines(lines);
        validateAccounts(request.userId(), lines);
        validateBalanced(lines);

        LedgerEntry entry = new LedgerEntry();
        entry.setUserId(request.userId());
        entry.setCategoryId(request.categoryId());
        entry.setEntryDate(request.entryDate());
        entry.setDescription(request.description());

        for (PostEntryLine line : lines) {
            TransactionEntry te = new TransactionEntry();
            te.setAccountId(line.accountId());
            te.setDirection(line.direction());
            te.setAmount(MoneyUtils.normalize(line.amount()));
            entry.addLine(te);
        }

        LedgerEntry saved = ledgerEntryRepository.save(entry);
        log.info("Posted ledger entry {} for user {} with {} lines", saved.getId(), request.userId(), lines.size());
        return saved;
    }

    private void validateHeader(PostEntryRequest request) {
        if (request == null || request.userId() == null) {
            throw new BadRequestException("User is required to post a ledger entry");
        }
        if (request.entryDate() == null) {
            throw new BadRequestException("Entry date is required");
        }
        if (request.categoryId() != null
                && categoryRepository.findByIdAndUserId(request.categoryId(), request.userId())
                .filter(c -> c.isActive()).isEmpty()) {
            throw new BadRequestException("Category not found or inactive");
        }
    }

    private void validateLines(List<PostEntryLine> lines) {
        if (lines == null || lines.size() < 2) {
            throw new BadRequestException("A ledger entry needs at least two lines");
        }
        for (PostEntryLine line : lines) {
            if (line == null || line.accountId() == null || line.direction() == null) {
                throw new BadRequestException("Every line needs an account and a direction");
            }
            BigDecimal amount = line.amount();
            if (amount == null || amount.stripTrailingZeros().scale() > MONETARY_SCALE) {
                throw new BadRequestException("Amounts must have at most " + MONETARY_SCALE + " decimal places");
            }
            if (!MoneyUtils.isPositive(MoneyUtils.normalize(amount))) {
                throw new BadRequestException("Every line amount must be greater than zero");
            }
        }
        boolean hasDebit = lines.stream().anyMatch(l -> l.direction() == EntryDirection.DEBIT);
        boolean hasCredit = lines.stream().anyMatch(l -> l.direction() == EntryDirection.CREDIT);
        if (!hasDebit || !hasCredit) {
            throw new BadRequestException("A ledger entry needs at least one debit and one credit");
        }
    }

    private void validateAccounts(Long userId, List<PostEntryLine> lines) {
        Set<Long> ids = lines.stream().map(PostEntryLine::accountId).collect(Collectors.toSet());
        Map<Long, Account> found = accountRepository.findAllByUserIdAndIdIn(userId, ids).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        if (found.size() != ids.size()) {
            throw new BadRequestException("One or more accounts do not exist for this user");
        }
        if (found.values().stream().anyMatch(a -> !a.isActive())) {
            throw new BadRequestException("One or more accounts are inactive");
        }
    }

    private void validateBalanced(List<PostEntryLine> lines) {
        BigDecimal debits = sum(lines, EntryDirection.DEBIT);
        BigDecimal credits = sum(lines, EntryDirection.CREDIT);
        if (!MoneyUtils.equals(debits, credits)) {
            throw new BadRequestException("Entry is not balanced: debits " + debits + " != credits " + credits);
        }
    }

    private BigDecimal sum(List<PostEntryLine> lines, EntryDirection direction) {
        return lines.stream()
                .filter(l -> l.direction() == direction)
                .map(PostEntryLine::amount)
                .reduce(BigDecimal.ZERO, MoneyUtils::add);
    }
}
