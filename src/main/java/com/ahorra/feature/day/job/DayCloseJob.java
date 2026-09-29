package com.ahorra.feature.day.job;

import com.ahorra.feature.day.service.DayCloseService;
import com.ahorra.feature.user.model.User;
import com.ahorra.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * The 11:59pm close-check. For each user it evaluates today and any day still
 * missed: clean days close (their summary can fire), days with unconfirmed
 * entries are suspended. One user failing never stops the rest.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DayCloseJob {

    private final UserRepository userRepository;
    private final DayCloseService dayCloseService;

    @Value("${app.timezone:Africa/Lagos}")
    private String timezone;

    @Scheduled(cron = "${app.day.close-cron:0 59 23 * * *}", zone = "${app.timezone:Africa/Lagos}")
    public void closeDay() {
        LocalDate today = LocalDate.now(ZoneId.of(timezone));
        log.info("Running day-close check for {}", today);
        for (User user : userRepository.findAll()) {
            try {
                dayCloseService.catchUp(user.getId(), today);
            } catch (RuntimeException ex) {
                log.error("Day-close check failed for user {}", user.getId(), ex);
            }
        }
    }
}
