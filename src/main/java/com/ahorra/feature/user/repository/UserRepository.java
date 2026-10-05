package com.ahorra.feature.user.repository;
import com.ahorra.feature.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumberOrEmailOrUsername(String phoneNumber, String email, String username);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByInternalAuthId(String keycloakId);
    Optional<User> findByUsername(String username);

    /** The users that have the daily reminder switched on. */
    @Query("select u.id from User u where u.reminderEnabled = true")
    List<Long> findReminderEnabledUserIds();
}
