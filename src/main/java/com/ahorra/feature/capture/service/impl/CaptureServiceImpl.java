package com.ahorra.feature.capture.service.impl;

import com.ahorra.common.exception.BadRequestException;
import com.ahorra.feature.accounting.dto.LookupItem;
import com.ahorra.feature.accounting.model.Account;
import com.ahorra.feature.accounting.model.Category;
import com.ahorra.feature.accounting.repository.AccountRepository;
import com.ahorra.feature.accounting.repository.CategoryRepository;
import com.ahorra.feature.accounting.service.impl.LookupCacheService;
import com.ahorra.feature.capture.dto.CaptureRequest;
import com.ahorra.feature.capture.dto.ParsedEntry;
import com.ahorra.feature.capture.dto.ParsedEntryProposal;
import com.ahorra.feature.capture.dto.ParsedItem;
import com.ahorra.feature.capture.dto.PendingEntryResponse;
import com.ahorra.feature.capture.enums.CaptureStatus;
import com.ahorra.feature.capture.enums.EntrySource;
import com.ahorra.feature.capture.enums.PendingStatus;
import com.ahorra.feature.capture.model.PendingEntry;
import com.ahorra.feature.capture.model.PendingEntryItem;
import com.ahorra.feature.capture.model.RawEntry;
import com.ahorra.feature.capture.parser.EntryParser;
import com.ahorra.feature.capture.parser.ParseContext;
import com.ahorra.feature.capture.repository.PendingEntryRepository;
import com.ahorra.feature.capture.repository.RawEntryRepository;
import com.ahorra.feature.capture.service.CaptureService;
import com.ahorra.feature.day.service.DayGateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.ahorra.common.constants.GeneralConstants.*;

/**
 * NOTE: deliberately NOT @Transactional. The raw entry must be committed
 * before the (slow) LLM call so it survives a failed parse, and no database
 * connection is held open while waiting on the network.
 */
@Slf4j
@Service
public class CaptureServiceImpl implements CaptureService {

    private final RawEntryRepository rawEntryRepository;
    private final PendingEntryRepository pendingEntryRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final LookupCacheService lookupCache;
    private final EntryParser entryParser;
    private final ParsedEntryValidator validator;
    private final DayGateService dayGateService;
    private final ZoneId zone;

    public CaptureServiceImpl(RawEntryRepository rawEntryRepository,
                              PendingEntryRepository pendingEntryRepository,
                              AccountRepository accountRepository,
                              CategoryRepository categoryRepository,
                              LookupCacheService lookupCache,
                              EntryParser entryParser,
                              ParsedEntryValidator validator,
                              DayGateService dayGateService,
                              @Value("${app.timezone:Africa/Lagos}") String timezone) {
        this.rawEntryRepository = rawEntryRepository;
        this.pendingEntryRepository = pendingEntryRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.lookupCache = lookupCache;
        this.entryParser = entryParser;
        this.validator = validator;
        this.dayGateService = dayGateService;
        this.zone = ZoneId.of(timezone);
    }

    @Override
    public PendingEntryResponse capture(Long userId, CaptureRequest request) {
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new BadRequestException("Entry text is required");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new BadRequestException("Entry is too long (max " + MAX_TEXT_LENGTH + " characters)");
        }
        String clientRequestId = normalizeClientRequestId(request.clientRequestId());

        // Forced-resolution gate: unconfirmed past days (or past the evening cutoff) block new capture.
        dayGateService.assertNotGated(userId);

        // 1. Persist exactly what the user said (or find it again on a retry).
        RawEntry raw = findOrCreateRaw(userId, request.source(), text, clientRequestId);

        // 2. Idempotent replay: this input already produced a pending entry.
        Optional<PendingEntry> existing = pendingEntryRepository.findByRawEntryId(raw.getId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // A retry of a previously failed parse starts from a clean status.
        if (raw.getStatus() != CaptureStatus.RECEIVED) {
            raw.setStatus(CaptureStatus.RECEIVED);
            raw.setFailureReason(null);
            raw = rawEntryRepository.save(raw);
        }

        // 3. Parse + validate + stage. Any failure is recorded on the raw entry.
        try {
            LocalDate today = LocalDate.now(zone);
            List<LookupItem> accounts = lookupCache.captureAccounts(userId);
            List<LookupItem> categories = lookupCache.categories(userId);

            ParsedEntry parsed = entryParser.parse(new ParseContext(
                    raw.getRawText(),
                    today,
                    accounts.stream().map(LookupItem::name).toList(),
                    categories.stream().map(LookupItem::name).toList()));

            ParsedEntryProposal proposal = validator.validate(raw.getId(), parsed, today, accounts, categories);
            PendingEntry pending = buildPending(userId, proposal);

            PendingEntry saved;
            try {
                saved = pendingEntryRepository.save(pending);
            } catch (DataIntegrityViolationException ex) {
                // A concurrent duplicate request won the race for this raw entry.
                return toResponse(pendingEntryRepository.findByRawEntryId(raw.getId()).orElseThrow(() -> ex));
            }

            raw.setStatus(CaptureStatus.PARSED);
            rawEntryRepository.save(raw);
            return toResponse(saved);
        } catch (RuntimeException ex) {
            markFailed(raw, ex.getMessage());
            throw ex;
        }
    }

    private String normalizeClientRequestId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_CLIENT_REQUEST_ID_LENGTH) {
            throw new BadRequestException("clientRequestId is too long");
        }
        return trimmed;
    }

    private RawEntry findOrCreateRaw(Long userId, EntrySource source, String text, String clientRequestId) {
        if (clientRequestId != null) {
            Optional<RawEntry> found = rawEntryRepository.findByUserIdAndClientRequestId(userId, clientRequestId);
            if (found.isPresent()) {
                return found.get();
            }
        }

        RawEntry raw = new RawEntry();
        raw.setUserId(userId);
        raw.setSource(source == null ? EntrySource.TEXT : source);
        raw.setRawText(text);
        raw.setStatus(CaptureStatus.RECEIVED);
        raw.setClientRequestId(clientRequestId);
        try {
            return rawEntryRepository.save(raw);
        } catch (DataIntegrityViolationException ex) {
            if (clientRequestId != null) {
                return rawEntryRepository.findByUserIdAndClientRequestId(userId, clientRequestId)
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
    }

    private PendingEntry buildPending(Long userId, ParsedEntryProposal proposal) {
        if (proposal.totalAmount().compareTo(MAX_TOTAL) >= 0) {
            throw new BadRequestException("Total amount is too large");
        }

        PendingEntry pending = new PendingEntry();
        pending.setUserId(userId);
        pending.setRawEntryId(proposal.rawEntryId());
        pending.setFlow(proposal.flow());
        pending.setEntryDate(proposal.entryDate());
        pending.setVendor(truncate(proposal.vendor(), MAX_VENDOR_LENGTH));
        pending.setCategoryId(proposal.categoryId());
        pending.setAccountId(proposal.accountId());
        pending.setTotalAmount(proposal.totalAmount());
        pending.setStatus(PendingStatus.PENDING);

        for (ParsedItem item : proposal.items()) {
            PendingEntryItem row = new PendingEntryItem();
            row.setDescription(item.description());
            row.setAmount(item.amount());
            pending.addItem(row);
        }
        return pending;
    }

    private PendingEntryResponse toResponse(PendingEntry pendingEntry) {
        String categoryName = pendingEntry.getCategoryId() == null ? null : categoryRepository.findById(pendingEntry.getCategoryId()).map(Category::getName).orElse(null);
        String accountName = pendingEntry.getAccountId() == null ? null : accountRepository.findById(pendingEntry.getAccountId()).map(Account::getName).orElse(null);
        List<ParsedItem> items = pendingEntry.getItems().stream()
                .map(i -> new ParsedItem(i.getDescription(), i.getAmount()))
                .toList();

        return new PendingEntryResponse(pendingEntry.getId(), pendingEntry.getRawEntryId(), pendingEntry.getStatus(), pendingEntry.getFlow(), pendingEntry.getEntryDate(), pendingEntry.getVendor(), pendingEntry.getCategoryId(), categoryName, pendingEntry.getAccountId(), accountName, items, pendingEntry.getTotalAmount());
    }

    private String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private void markFailed(RawEntry raw, String reason) {
        try {
            raw.setStatus(CaptureStatus.FAILED);
            raw.setFailureReason(reason == null ? null : reason.substring(0, Math.min(reason.length(), 500)));
            rawEntryRepository.save(raw);
        } catch (RuntimeException ex) {
            log.error("Could not mark raw entry {} as failed", raw.getId(), ex);
        }
    }
}
