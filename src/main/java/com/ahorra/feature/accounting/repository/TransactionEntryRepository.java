package com.ahorra.feature.accounting.repository;

import com.ahorra.feature.accounting.model.TransactionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionEntryRepository extends JpaRepository<TransactionEntry, Long> {
}
