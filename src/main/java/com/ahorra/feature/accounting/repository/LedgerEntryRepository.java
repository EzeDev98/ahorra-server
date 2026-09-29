package com.ahorra.feature.accounting.repository;

import com.ahorra.feature.accounting.enums.AccountType;
import com.ahorra.feature.accounting.enums.EntryDirection;
import com.ahorra.feature.accounting.model.LedgerEntry;
import com.ahorra.feature.day.dto.DaySpend;
import com.ahorra.feature.income.dto.CategorySpendRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    Optional<LedgerEntry> findByIdAndUserId(Long id, Long userId);
    List<LedgerEntry> findAllByUserIdAndEntryDate(Long userId, LocalDate entryDate);

    /** Number of posted ledger entries (any kind) on a day. */
    long countByUserIdAndEntryDate(Long userId, LocalDate entryDate);

    /**
     * Confirmed spend per day: the sum of DEBIT lines on the user's expense
     * accounts. Only days that have spend appear; zero-filling is done by
     * DailySpendService.
     */
    @Query("""
            select new com.ahorra.feature.day.dto.DaySpend(e.entryDate, sum(t.amount))
            from TransactionEntry t join t.ledgerEntry e
            where e.userId = :userId
              and e.entryDate between :from and :to
              and t.direction = :direction
              and t.accountId in (select a.id from Account a where a.userId = :userId and a.type = :accountType)
            group by e.entryDate
            """)
    List<DaySpend> sumSpendByDay(@Param("userId") Long userId,
                                 @Param("accountType") AccountType accountType,
                                 @Param("direction") EntryDirection direction,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);

    /** Confirmed spend per category over a date range (null category id = uncategorized). */
    @Query("""
            select new com.ahorra.feature.income.dto.CategorySpendRow(e.categoryId, sum(t.amount))
            from TransactionEntry t join t.ledgerEntry e
            where e.userId = :userId
              and e.entryDate between :from and :to
              and t.direction = :direction
              and t.accountId in (select a.id from Account a where a.userId = :userId and a.type = :accountType)
            group by e.categoryId
            """)
    List<CategorySpendRow> sumSpendByCategory(@Param("userId") Long userId,
                                              @Param("accountType") AccountType accountType,
                                              @Param("direction") EntryDirection direction,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);
}
