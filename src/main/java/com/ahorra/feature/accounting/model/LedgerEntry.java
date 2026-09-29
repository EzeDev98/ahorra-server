package com.ahorra.feature.accounting.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A posted double-entry transaction (the header). Its lines live in
 * {@link TransactionEntry}. Only confirmed entries ever reach this table;
 * pending/unconfirmed input is staged elsewhere.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Optional category tag for the whole transaction. */
    @Column(name = "category_id")
    private Long categoryId;

    /** The calendar day the transaction belongs to (drives day-close and reporting). */
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @OneToMany(mappedBy = "ledgerEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionEntry> lines = new ArrayList<>();

    public void addLine(TransactionEntry line) {
        line.setLedgerEntry(this);
        lines.add(line);
    }
}
