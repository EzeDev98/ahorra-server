package com.ahorra.feature.day.service.impl;

import com.ahorra.common.exception.UserNotFoundException;
import com.ahorra.feature.capture.enums.PendingStatus;
import com.ahorra.feature.capture.repository.PendingEntryRepository;
import com.ahorra.feature.day.enums.DayStatus;
import com.ahorra.feature.day.event.DayClosedEvent;
import com.ahorra.feature.day.model.DayState;
import com.ahorra.feature.day.repository.DayStateRepository;
import com.ahorra.feature.day.service.DayCloseService;
import com.ahorra.feature.user.model.User;
import com.ahorra.feature.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class DayCloseServiceImpl implements DayCloseService {

    private final DayStateRepository dayStateRepository;
    private final PendingEntryRepository pendingEntryRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ZoneId zone;

    public DayCloseServiceImpl(DayStateRepository dayStateRepository,
                               PendingEntryRepository pendingEntryRepository,
                               UserRepository userRepository,
                               ApplicationEventPublisher eventPublisher,
                               @Value("${app.timezone:Africa/Lagos}") String timezone) {
        this.dayStateRepository = dayStateRepository;
        this.pendingEntryRepository = pendingEntryRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.zone = ZoneId.of(timezone);
    }

    @Override
    @Transactional
    public void catchUp(Long userId, LocalDate through) {
        Set<LocalDate> pendingDays = new HashSet<>(pendingEntryRepository.findEntryDatesByStatus(userId, PendingStatus.PENDING));

        // 1. Suspended days whose backlog is now cleared close retroactively.
        for (DayState suspended : dayStateRepository.findAllByUserIdAndStatus(userId, DayStatus.SUSPENDED)) {
            if (!pendingDays.contains(suspended.getDayDate())) {
                suspended.setStatus(DayStatus.CLOSED);
                suspended.setClosedAt(LocalDateTime.now());
                dayStateRepository.save(suspended);
                eventPublisher.publishEvent(new DayClosedEvent(userId, suspended.getDayDate(), true));
                log.info("Day {} closed retroactively for user {}", suspended.getDayDate(), userId);
            }
        }

        // 2. Evaluate every day that has no row yet, oldest first.
        LocalDate next = dayStateRepository.findTopByUserIdOrderByDayDateDesc(userId)
                .map(dayState -> dayState.getDayDate().plusDays(1))
                .orElseGet(() -> firstDay(userId));

        for (LocalDate day = next; !day.isAfter(through); day = day.plusDays(1)) {
            DayState state = new DayState();
            state.setUserId(userId);
            state.setDayDate(day);
            if (pendingDays.contains(day)) {
                state.setStatus(DayStatus.SUSPENDED);
                state.setSuspendedAt(LocalDateTime.now());
                dayStateRepository.save(state);
                log.info("Day {} suspended for user {}: unconfirmed entries remain", day, userId);
            } else {
                state.setStatus(DayStatus.CLOSED);
                state.setClosedAt(LocalDateTime.now());
                dayStateRepository.save(state);
                eventPublisher.publishEvent(new DayClosedEvent(userId, day, false));
            }
        }
    }

    @Override
    public boolean isClosed(Long userId, LocalDate day) {
        return dayStateRepository.existsByUserIdAndDayDateAndStatus(userId, day, DayStatus.CLOSED);
    }

    private LocalDate firstDay(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return user.getCreatedAt() == null ? LocalDate.now(zone) : user.getCreatedAt().toLocalDate();
    }
}
