package com.ahorra.feature.accounting.model;

import com.ahorra.feature.accounting.enums.PeriodType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.ahorra.common.money.MoneyConfig.MONETARY_PRECISION;
import static com.ahorra.common.money.MoneyConfig.MONETARY_SCALE;

/**
 * A dated record of a user's stated income range. Income is history, never a
 * single mutable row: changing income closes the current record and inserts a
 * new one, so past reports always use the income that was active at the time.
 *
 * <p>{@code effectiveTo} is EXCLUSIVE. A record applies on day {@code d} when
 * {@code effectiveFrom <= d < effectiveTo} (or {@code effectiveTo} is null,
 * meaning it is the current record). Closing the old record with
 * {@code effectiveTo = today} and opening the new one with
 * {@code effectiveFrom = today} therefore never overlaps.
 *
 * <p>The lower bound ({@code minAmount}) is the baseline used for judgment;
 * the full range is kept for display.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "income_history")
public class IncomeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "min_amount", nullable = false, precision = MONETARY_PRECISION, scale = MONETARY_SCALE)
    private BigDecimal minAmount;

    @Column(name = "max_amount", nullable = false, precision = MONETARY_PRECISION, scale = MONETARY_SCALE)
    private BigDecimal maxAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private PeriodType periodType;

    /** Length of the period in days. Required when periodType is CUSTOM, otherwise null. */
    @Column(name = "period_length")
    private Integer periodLength;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Exclusive end date. Null means this is the current record. */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
