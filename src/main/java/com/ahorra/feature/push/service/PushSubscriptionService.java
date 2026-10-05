package com.ahorra.feature.push.service;

import com.ahorra.feature.push.dto.PushSubscriptionRequest;

public interface PushSubscriptionService {

    /** Saves the device for the user. The same device subscribing again replaces its old record, whoever owned it. */
    void subscribe(Long userId, PushSubscriptionRequest request);

    /** Removes the device for the user. Unknown endpoints are ignored. */
    void unsubscribe(Long userId, String endpoint);

    /** Whether the user has the daily reminder switched on. Kept on the user, so it survives a reinstall. */
    boolean isReminderEnabled(Long userId);

    void setReminderEnabled(Long userId, boolean enabled);
}
