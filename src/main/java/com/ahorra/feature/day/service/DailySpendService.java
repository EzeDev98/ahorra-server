package com.ahorra.feature.day.service;

import com.ahorra.common.money.MoneyUtils;
import com.ahorra.feature.accounting.enums.AccountType;
import com.ahorra.feature.accounting.enums.EntryDirection;
import com.ahorra.feature.accounting.repository.LedgerEntryRepository;
import com.ahorra.feature.day.dto.DaySpend;
import com.ahorra.feature.day.enums.DayStatus;
import com.ahorra.feature.day.repository.DayStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The daily spend series that averages and trends are built from. It returns
 * one value for EVERY closed day in the range, with 0.00 for days that had
 * nothing logged. Those zeros are genuine data points: leaving them out would
 * only ever average over days that had spending and inflate the "usual" figure.
 *
 * <p>Open and suspended days are excluded because they are not final yet.
 */
@Service
@RequiredArgsConstructor
public class DailySpendService {

    private final DayStateRepository dayStateRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public List<DaySpend> closedDaySpend(Long userId, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> spendByDay = ledgerEntryRepository
                .sumSpendByDay(userId, AccountType.EXPENSE, EntryDirection.DEBIT, from, to).stream()
                .collect(Collectors.toMap(DaySpend::day, DaySpend::spend));

        return dayStateRepository
                .findAllByUserIdAndStatusAndDayDateBetweenOrderByDayDateAsc(userId, DayStatus.CLOSED, from, to)
                .stream()
                .map(state -> new DaySpend(state.getDayDate(),
                        MoneyUtils.normalize(spendByDay.getOrDefault(state.getDayDate(), BigDecimal.ZERO))))
                .toList();
    }
}
