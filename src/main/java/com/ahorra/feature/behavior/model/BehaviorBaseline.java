package com.ahorra.feature.behavior.model;

import com.ahorra.feature.behavior.enums.DayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.ahorra.common.money.MoneyConfig.MONETARY_PRECISION;
import static com.ahorra.common.money.MoneyConfig.MONETARY_SCALE;

/**
 * Rolling mean and standard deviation of daily spend for one category, window
 * and day type. Recomputed and updated in place; zero-spend closed days count
 * as real zero data points.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "behavior_baselines")
public class BehaviorBaseline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** A real category id, or a BehaviorScope sentinel. */
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "window_days", nullable = false)
    private int windowDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 10)
    private DayType dayType;

    @Column(name = "mean_amount", nullable = false, precision = MONETARY_PRECISION, scale = MONETARY_SCALE)
    private BigDecimal meanAmount;

    @Column(name = "std_dev", nullable = false, precision = MONETARY_PRECISION, scale = MONETARY_SCALE)
    private BigDecimal stdDev;

    /** Closed days that went into this row. */
    @Column(name = "sample_days", nullable = false)
    private int sampleDays;

    /** min(days observed / 30, 1). */
    @Column(name = "confidence", nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "computed_through", nullable = false)
    private LocalDate computedThrough;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
