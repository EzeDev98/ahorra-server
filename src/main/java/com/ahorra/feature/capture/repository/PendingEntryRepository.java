package com.ahorra.feature.capture.repository;

import com.ahorra.feature.capture.enums.FlowDirection;
import com.ahorra.feature.capture.enums.PendingStatus;
import com.ahorra.feature.capture.model.PendingEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PendingEntryRepository extends JpaRepository<PendingEntry, Long> {

    @EntityGraph(attributePaths = "items")
    List<PendingEntry> findAllByRawEntryIdOrderByIdAsc(Long rawEntryId);

    @EntityGraph(attributePaths = "items")
    Optional<PendingEntry> findByIdAndUserId(Long id, Long userId);

    /** The user's queue in the order it should be cleared: oldest day first. */
    @EntityGraph(attributePaths = "items")
    List<PendingEntry> findAllByUserIdAndStatusOrderByEntryDateAscIdAsc(Long userId, PendingStatus status);

    /**
     * Row-locks the entry so two simultaneous confirms cannot both post it.
     * Items are deliberately not fetched here (FOR UPDATE cannot combine with
     * the outer join a fetch would need); they load lazily inside the transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PendingEntry p where p.id = :id and p.userId = :userId")
    Optional<PendingEntry> findForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    /** Distinct days that still have unconfirmed entries. */
    @Query("select distinct p.entryDate from PendingEntry p where p.userId = :userId and p.status = :status")
    List<LocalDate> findEntryDatesByStatus(@Param("userId") Long userId, @Param("status") PendingStatus status);

    /** Sum of unconfirmed amounts for one day and flow. Null when there are none. */
    @Query("select sum(p.totalAmount) from PendingEntry p "
            + "where p.userId = :userId and p.status = :status and p.flow = :flow and p.entryDate = :day")
    BigDecimal sumTotalByStatusAndFlowAndDay(@Param("userId") Long userId,
                                             @Param("status") PendingStatus status,
                                             @Param("flow") FlowDirection flow,
                                             @Param("day") LocalDate day);

    long countByUserIdAndStatusAndEntryDate(Long userId, PendingStatus status, LocalDate entryDate);
}
