package com.ahorra.feature.accounting.enums;

/**
 * The five standard classes of account in a chart of accounts.
 * ASSET covers both cash and bank accounts.
 */
public enum AccountType {
    ASSET(true),
    EXPENSE(true),
    LIABILITY(false),
    EQUITY(false),
    INCOME(false);

    private final boolean debitNormal;

    AccountType(boolean debitNormal) {
        this.debitNormal = debitNormal;
    }

    /** True when a debit increases this account's balance, false when a credit does. */
    public boolean isDebitNormal() {
        return debitNormal;
    }
}
