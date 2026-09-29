package com.ahorra.feature.capture.model;

import com.ahorra.feature.capture.enums.FlowDirection;
import com.ahorra.feature.capture.enums.PendingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.ahorra.common.money.MoneyConfig.MONETARY_PRECISION;
import static com.ahorra.common.money.MoneyConfig.MONETARY_SCALE;

/**
 * A parsed entry staged for the user's confirmation. It is NOT part of the
 * ledger: only confirming it creates real double-entry rows. Always tied to
 * the raw input it was parsed from.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pending_entries")
public class PendingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "raw_entry_id", nullable = false, unique = true)
    private Long rawEntryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "flow", nullable = false, length = 10)
    private FlowDirection flow;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "vendor")
    private String vendor;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "total_amount", nullable = false, precision = MONETARY_PRECISION, scale = MONETARY_SCALE)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PendingStatus status = PendingStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "pendingEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PendingEntryItem> items = new ArrayList<>();

    public void addItem(PendingEntryItem item) {
        item.setPendingEntry(this);
        item.setPosition(items.size());
        items.add(item);
    }
}
