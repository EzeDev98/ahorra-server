package com.ahorra.feature.push.repository;

import com.ahorra.feature.push.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    List<PushSubscription> findAllByUserIdOrderByIdAsc(Long userId);
    void deleteByEndpointAndUserId(String endpoint, Long userId);
    void deleteByEndpoint(String endpoint);

    /** The users that have at least one device to notify. */
    @Query("select distinct s.userId from PushSubscription s")
    List<Long> findSubscribedUserIds();
}
