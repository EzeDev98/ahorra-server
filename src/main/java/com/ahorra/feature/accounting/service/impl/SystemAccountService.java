package com.ahorra.feature.accounting.service.impl;

import com.ahorra.common.exception.BadRequestException;
import com.ahorra.feature.accounting.enums.AccountType;
import com.ahorra.feature.accounting.model.Account;
import com.ahorra.feature.accounting.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Provides each user's default accounts, creating them on first use so the
 * user never has to set up a chart of accounts.
 */
@Service
@RequiredArgsConstructor
public class SystemAccountService {

    public static final String CASH = "Cash";
    public static final String EXPENSES = "Expenses";
    public static final String INCOME = "Income";

    private final AccountRepository accountRepository;

    public Account cash(Long userId) {
        return getOrCreate(userId, CASH, AccountType.ASSET);
    }

    public Account expenses(Long userId) {
        return getOrCreate(userId, EXPENSES, AccountType.EXPENSE);
    }

    public Account income(Long userId) {
        return getOrCreate(userId, INCOME, AccountType.INCOME);
    }

    private Account getOrCreate(Long userId, String name, AccountType type) {
        Optional<Account> existing = accountRepository.findByUserIdAndName(userId, name);
        if (existing.isPresent()) {
            Account account = existing.get();
            if (account.getType() != type || !account.isActive()) {
                throw new BadRequestException("Account '" + name + "' exists but cannot be used as the default " + type + " account");
            }
            return account;
        }

        Account account = new Account();
        account.setUserId(userId);
        account.setName(name);
        account.setType(type);
        account.setActive(true);
        account.setSystem(true);
        return accountRepository.save(account);
    }
}
