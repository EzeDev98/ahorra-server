package com.ahorra.feature.accounting.repository;

import com.ahorra.feature.accounting.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByIdAndUserId(Long id, Long userId);
    List<Account> findAllByUserIdAndIdIn(Long userId, Collection<Long> ids);
    List<Account> findAllByUserIdAndActiveTrue(Long userId);
    boolean existsByUserIdAndName(Long userId, String name);
    Optional<Account> findByUserIdAndName(Long userId, String name);
}
