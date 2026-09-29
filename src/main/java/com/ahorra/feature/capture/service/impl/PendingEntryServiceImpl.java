package com.ahorra.feature.capture.service.impl;

import com.ahorra.common.exception.BadRequestException;
import com.ahorra.feature.accounting.dto.PostEntryLine;
import com.ahorra.feature.accounting.dto.PostEntryRequest;
import com.ahorra.feature.accounting.enums.AccountType;
import com.ahorra.feature.accounting.enums.EntryDirection;
import com.ahorra.feature.accounting.model.Account;
import com.ahorra.feature.accounting.model.Category;
import com.ahorra.feature.accounting.model.LedgerEntry;
import com.ahorra.feature.accounting.repository.AccountRepository;
import com.ahorra.feature.accounting.repository.CategoryRepository;
import com.ahorra.feature.accounting.service.LedgerService;
import com.ahorra.feature.accounting.service.impl.SystemAccountService;
import com.ahorra.feature.capture.dto.ConfirmRequest;
import com.ahorra.feature.capture.dto.ParsedItem;
import com.ahorra.feature.capture.dto.PendingEntryResponse;
import com.ahorra.feature.capture.enums.FlowDirection;
import com.ahorra.feature.capture.enums.PendingStatus;
import com.ahorra.feature.capture.model.PendingEntry;
import com.ahorra.feature.capture.model.PendingEntryItem;
import com.ahorra.feature.capture.repository.PendingEntryRepository;
import com.ahorra.feature.capture.service.PendingEntryService;
import com.ahorra.feature.day.service.DayCloseService;
import com.ahorra.common.money.MoneyUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PendingEntryServiceImpl implements PendingEntryService {

    private static final int MAX_VENDOR_LENGTH = 255;
    private static final int MAX_LEDGER_DESCRIPTION_LENGTH = 500;
    /** Must stay below the NUMERIC(15,2) ceiling of the total_amount column. */
    private static final BigDecimal MAX_TOTAL = new BigDecimal("10000000000000");

    private final PendingEntryRepository pendingEntryRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final SystemAccountService systemAccounts;
    private final LedgerService ledgerService;
    private final ParsedEntryValidator validator;
    private final PendingEntryMapper mapper;
    private final DayCloseService dayCloseService;
    private final ZoneId zone;

    public PendingEntryServiceImpl(PendingEntryRepository pendingEntryRepository,
                                   AccountRepository accountRepository,
                                   CategoryRepository categoryRepository,
                                   SystemAccountService systemAccounts,
                                   LedgerService ledgerService,
                                   ParsedEntryValidator validator,
                                   PendingEntryMapper mapper,
                                   DayCloseService dayCloseService,
                                   @Value("${app.timezone:Africa/Lagos}") String timezone) {
        this.pendingEntryRepository = pendingEntryRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.systemAccounts = systemAccounts;
        this.ledgerService = ledgerService;
        this.validator = validator;
        this.mapper = mapper;
        this.dayCloseService = dayCloseService;
        this.zone = ZoneId.of(timezone);
    }

    @Override
    public List<PendingEntryResponse> listPending(Long userId) {
        return mapper.toResponses(
                pendingEntryRepository.findAllByUserIdAndStatusOrderByEntryDateAscIdAsc(userId, PendingStatus.PENDING));
    }

    @Override
    @Transactional
    public PendingEntryResponse confirm(Long userId, Long pendingEntryId, ConfirmRequest edits) {
        PendingEntry pending = pendingEntryRepository.findForUpdate(pendingEntryId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Pending entry not found"));
        boolean edited = edits != null && edits.hasEdits();

        if (pending.getStatus() != PendingStatus.PENDING) {
            if (edited) {
                throw new BadRequestException("This entry has already been confirmed and can no longer be edited");
            }
            return mapper.toResponse(pending);
        }

        if (edited) {
            applyEdits(userId, pending, edits);
        }

        if (dayCloseService.isClosed(userId, pending.getEntryDate())) {
            throw new BadRequestException("That day is already closed. Change the entry's date to confirm it.");
        }

        LedgerEntry ledgerEntry = ledgerService.postEntry(buildPostRequest(userId, pending));
        pending.markPosted(ledgerEntry.getId(), edited);
        pendingEntryRepository.save(pending);

        dayCloseService.catchUp(userId, LocalDate.now(zone).minusDays(1));

        log.info("Pending entry {} posted as ledger entry {} for user {}", pending.getId(), ledgerEntry.getId(), userId);
        return mapper.toResponse(pending);
    }

    private void applyEdits(Long userId, PendingEntry pending, ConfirmRequest edits) {
        if (edits.flow() != null) {
            pending.setFlow(edits.flow());
        }
        if (edits.entryDate() != null) {
            if (edits.entryDate().isAfter(LocalDate.now(zone))) {
                throw new BadRequestException("Entry date cannot be in the future");
            }
            pending.setEntryDate(edits.entryDate());
        }
        if (edits.vendor() != null) {
            String vendor = edits.vendor().trim();
            pending.setVendor(vendor.isEmpty() ? null : truncate(vendor, MAX_VENDOR_LENGTH));
        }
        if (edits.categoryId() != null) {
            categoryRepository.findByIdAndUserId(edits.categoryId(), userId)
                    .filter(Category::isActive)
                    .orElseThrow(() -> new BadRequestException("Category not found or inactive"));
            pending.setCategoryId(edits.categoryId());
        }
        if (edits.accountId() != null) {
            accountRepository.findByIdAndUserId(edits.accountId(), userId)
                    .filter(Account::isActive)
                    .filter(a -> a.getType() == AccountType.ASSET)
                    .orElseThrow(() -> new BadRequestException("Account not found, inactive, or not a cash/bank account"));
            pending.setAccountId(edits.accountId());
        }
        if (edits.items() != null) {
            List<ParsedItem> clean = validator.validateItems(edits.items());
            BigDecimal total = clean.stream().map(ParsedItem::amount).reduce(BigDecimal.ZERO, MoneyUtils::add);
            if (total.compareTo(MAX_TOTAL) >= 0) {
                throw new BadRequestException("Total amount is too large");
            }
            pending.getItems().clear();
            for (ParsedItem item : clean) {
                PendingEntryItem row = new PendingEntryItem();
                row.setDescription(item.description());
                row.setAmount(item.amount());
                pending.addItem(row);
            }
            pending.setTotalAmount(total);
        }
    }

    private PostEntryRequest buildPostRequest(Long userId, PendingEntry pending) {
        Long paymentAccountId = pending.getAccountId() != null
                ? pending.getAccountId()
                : systemAccounts.cash(userId).getId();
        BigDecimal total = pending.getTotalAmount();

        List<PostEntryLine> lines;
        if (pending.getFlow() == FlowDirection.EXPENSE) {
            Long expensesId = systemAccounts.expenses(userId).getId();
            lines = List.of(
                    new PostEntryLine(expensesId, EntryDirection.DEBIT, total),
                    new PostEntryLine(paymentAccountId, EntryDirection.CREDIT, total));
        } else {
            Long incomeId = systemAccounts.income(userId).getId();
            lines = List.of(
                    new PostEntryLine(paymentAccountId, EntryDirection.DEBIT, total),
                    new PostEntryLine(incomeId, EntryDirection.CREDIT, total));
        }

        return new PostEntryRequest(userId, pending.getCategoryId(), pending.getEntryDate(),
                buildDescription(pending), lines);
    }

    private String buildDescription(PendingEntry pending) {
        String items = pending.getItems().stream()
                .map(PendingEntryItem::getDescription)
                .collect(Collectors.joining(", "));
        String description = pending.getVendor() == null ? items : pending.getVendor() + " - " + items;
        return truncate(description, MAX_LEDGER_DESCRIPTION_LENGTH);
    }

    private String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
