package com.ahorra.feature.accounting.service;

import com.ahorra.common.cache.RedisJsonCache;
import com.ahorra.feature.accounting.dto.LookupItem;
import com.ahorra.feature.accounting.enums.AccountType;
import com.ahorra.feature.accounting.model.Account;
import com.ahorra.feature.accounting.model.Category;
import com.ahorra.feature.accounting.repository.AccountRepository;
import com.ahorra.feature.accounting.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Cached copy of the names the AI parse step matches against: the user's active
 * asset accounts and active categories. Entries expire after a few minutes, and
 * anything that changes accounts or categories should call evict(userId).
 * A snapshot with no accounts is never cached, so a brand-new user does not get
 * stuck with an empty list.
 */
@Service
public class LookupCacheService {

    private static final Duration TTL = Duration.ofMinutes(5);

    public record LookupSnapshot(List<LookupItem> captureAccounts, List<LookupItem> categories) {
    }

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final RedisJsonCache cache;

    public LookupCacheService(AccountRepository accountRepository,
                              CategoryRepository categoryRepository,
                              RedisJsonCache cache) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.cache = cache;
    }

    public List<LookupItem> captureAccounts(Long userId) {
        return snapshot(userId).captureAccounts();
    }

    public List<LookupItem> categories(Long userId) {
        return snapshot(userId).categories();
    }

    public void evict(Long userId) {
        cache.evict(key(userId));
    }

    private LookupSnapshot snapshot(Long userId) {
        String key = key(userId);
        Optional<LookupSnapshot> cached = cache.get(key, LookupSnapshot.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        LookupSnapshot fresh = load(userId);
        if (!fresh.captureAccounts().isEmpty()) {
            cache.put(key, fresh, TTL);
        }
        return fresh;
    }

    private LookupSnapshot load(Long userId) {
        List<LookupItem> accounts = accountRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .filter(a -> a.getType() == AccountType.ASSET)
                .map(this::toItem)
                .toList();
        List<LookupItem> categories = categoryRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .map(this::toItem)
                .toList();
        return new LookupSnapshot(accounts, categories);
    }

    private LookupItem toItem(Account a) {
        return new LookupItem(a.getId(), a.getName());
    }

    private LookupItem toItem(Category c) {
        return new LookupItem(c.getId(), c.getName());
    }

    private String key(Long userId) {
        return "ahorra:lookups:v1:" + userId;
    }
}
