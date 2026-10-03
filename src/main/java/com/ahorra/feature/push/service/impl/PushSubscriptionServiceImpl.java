package com.ahorra.feature.push.service.impl;

import com.ahorra.feature.push.dto.PushSubscriptionRequest;
import com.ahorra.feature.push.model.PushSubscription;
import com.ahorra.feature.push.repository.PushSubscriptionRepository;
import com.ahorra.feature.push.service.PushSubscriptionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.ahorra.common.constants.GeneralConstants.MAX_PUSH_DEVICES;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushSubscriptionServiceImpl implements PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Override
    @Transactional
    public void subscribe(Long userId, PushSubscriptionRequest request) {
        PushSubscription subscription = pushSubscriptionRepository.findByEndpoint(request.endpoint())
                .orElseGet(PushSubscription::new);
        subscription.setUserId(userId);
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dh(request.keys().p256dh());
        subscription.setAuth(request.keys().auth());
        pushSubscriptionRepository.save(subscription);

        // Keep the newest devices only, so a user cannot pile up records.
        List<PushSubscription> devices = pushSubscriptionRepository.findAllByUserIdOrderByIdAsc(userId);
        for (int i = 0; i < devices.size() - MAX_PUSH_DEVICES; i++) {
            pushSubscriptionRepository.delete(devices.get(i));
        }
    }

    @Override
    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        pushSubscriptionRepository.deleteByEndpointAndUserId(endpoint, userId);
    }
}
