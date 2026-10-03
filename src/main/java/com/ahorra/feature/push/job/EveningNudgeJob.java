package com.ahorra.feature.push.job;

import com.ahorra.feature.capture.enums.PendingStatus;
import com.ahorra.feature.capture.repository.PendingEntryRepository;
import com.ahorra.feature.push.repository.PushSubscriptionRepository;
import com.ahorra.feature.push.service.impl.PushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The one daily nudge. Each evening, before the hard gate and the day close, every user with a registered device and
 * entries still waiting for confirmation gets a single notification. Users with nothing waiting get nothing. The words
 * describe; they never judge. One user failing never stops the rest.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EveningNudgeJob {

    static final String TITLE = "Ahorra";
    static final String BODY = "You have entries waiting to be confirmed before the day closes.";

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PendingEntryRepository pendingEntryRepository;
    private final PushSender pushSender;

    @Scheduled(cron = "${app.push.nudge-cron:0 0 21 * * *}", zone = "${app.timezone:Africa/Lagos}")
    public void sendNudges() {
        if (!pushSender.isEnabled()) {
            return;
        }
        int notified = 0;
        for (Long userId : pushSubscriptionRepository.findSubscribedUserIds()) {
            try {
                if (pendingEntryRepository.findEntryDatesByStatus(userId, PendingStatus.PENDING).isEmpty()) {
                    continue;
                }
                if (pushSender.send(userId, TITLE, BODY) > 0) {
                    notified++;
                }
            } catch (RuntimeException ex) {
                log.error("Evening nudge failed for user {}", userId, ex);
            }
        }
        log.info("Evening nudge sent to {} users", notified);
    }
}
