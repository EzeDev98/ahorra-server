package com.ahorra.feature.session.repository;

import com.ahorra.feature.session.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByToken(String token);
    Optional<UserSession> findByPhoneNumberAndIsActiveTrue(String phoneNumber);
    Optional<UserSession> findByPhoneNumber(String phoneNumber);
    boolean existsByUserIdAndIsActiveTrue(Long userId);
}