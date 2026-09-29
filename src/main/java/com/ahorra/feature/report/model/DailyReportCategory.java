package com.ahorra.feature.report.model;

import com.ahorra.feature.report.enums.CategoryScope;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

import static com.ahorra.common.money.MoneyConfig.MONETARY_PRECISION;
import static com.ahorra.common.money.MoneyConfig.MONETARY_SCALE;

/** One category line of a stored report, for either the day or the whole period. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "daily_report_categories")
public class DailyReportCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_report_id", nullable = false)
    private DailyReport report;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 10)
    private CategoryScope scope;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "amount", nullable = false, precision = MONETARY_PRECISION, scale = MONETARY_SCALE)
    private BigDecimal amount;

    @Column(name = "share_percent", nullable = false, precision = 6, scale = 2)
    private BigDecimal sharePercent;

    /** Only set for PERIOD lines: this category's proportional share of the overspend. */
    @Column(name = "attributed_excess", precision = MONETARY_PRECISION, scale = MONETARY_SCALE)
    private BigDecimal attributedExcess;
}
