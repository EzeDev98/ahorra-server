package com.ahorra.feature.pin.repository;

import com.ahorra.feature.pin.model.UserPin;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserPinRepository extends JpaRepository<UserPin, Long> {

    /** Row-locked read, so concurrent wrong attempts cannot slip past the attempt counter. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserPin p where p.userId = :userId")
    Optional<UserPin> findForUpdate(@Param("userId") Long userId);
}
