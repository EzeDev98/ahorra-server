package com.ahorra.feature.day.service.impl;

import com.ahorra.common.exception.DayGatedException;
import com.ahorra.feature.capture.enums.PendingStatus;
import com.ahorra.feature.capture.model.PendingEntry;
import com.ahorra.feature.capture.repository.PendingEntryRepository;
import com.ahorra.feature.capture.service.impl.PendingEntryMapper;
import com.ahorra.feature.day.dto.DayGate;
import com.ahorra.feature.day.dto.DayStatusResponse;
import com.ahorra.feature.day.enums.GateReason;
import com.ahorra.feature.day.service.DayCloseService;
import com.ahorra.feature.day.service.DayGateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * The gate is derived from live data on every call, never from a stored flag,
 * so it cannot be sidestepped by a missed job, going offline, or force-quitting.
 * The oldest day with unconfirmed entries always comes first.
 */
@Service
public class DayGateServiceImpl implements DayGateService {

    private final PendingEntryRepository pendingEntryRepository;
    private final PendingEntryMapper mapper;
    private final DayCloseService dayCloseService;
    private final ZoneId zone;
    private final LocalTime hardGateTime;

    public DayGateServiceImpl(PendingEntryRepository pendingEntryRepository,
                              PendingEntryMapper mapper,
                              DayCloseService dayCloseService,
                              @Value("${app.timezone:Africa/Lagos}") String timezone,
                              @Value("${app.day.hard-gate-time:22:00}") String hardGateTime) {
        this.pendingEntryRepository = pendingEntryRepository;
        this.mapper = mapper;
        this.dayCloseService = dayCloseService;
        this.zone = ZoneId.of(timezone);
        this.hardGateTime = LocalTime.parse(hardGateTime);
    }

    @Override
    public DayGate evaluate(Long userId) {
        LocalDate today = LocalDate.now(zone);
        LocalTime now = LocalTime.now(zone);

        List<LocalDate> days = pendingEntryRepository.findEntryDatesByStatus(userId, PendingStatus.PENDING)
                .stream().sorted().toList();

        List<LocalDate> pastDays = days.stream().filter(date -> date.isBefore(today)).toList();
        if (!pastDays.isEmpty()) {
            return new DayGate(true, GateReason.SUSPENDED_DAY, pastDays.get(0), pastDays.size() - 1);
        }
        if (days.contains(today) && !now.isBefore(hardGateTime)) {
            return new DayGate(true, GateReason.HARD_GATE_TODAY, today, 0);
        }
        return DayGate.open();
    }

    @Override
    public void assertNotGated(Long userId) {
        DayGate gate = evaluate(userId);
        if (!gate.gated()) {
            return;
        }
        String message = gate.reason() == GateReason.SUSPENDED_DAY ? "Please confirm your pending entries for " + gate.day() + " before continuing." : "It is past " + hardGateTime + ". Please confirm your pending entries before logging more.";
        throw new DayGatedException(message);
    }

    @Override
    public DayStatusResponse status(Long userId) {
        LocalDate today = LocalDate.now(zone);
        dayCloseService.catchUp(userId, today.minusDays(1));

        DayGate gate = evaluate(userId);
        List<PendingEntry> all = pendingEntryRepository
                .findAllByUserIdAndStatusOrderByEntryDateAscIdAsc(userId, PendingStatus.PENDING);
        List<PendingEntry> shown = gate.gated() ? all.stream().filter(pendingEntry -> pendingEntry.getEntryDate().equals(gate.day())).toList() : all;

        return new DayStatusResponse(gate.gated(), gate.reason(), gate.day(), gate.otherDaysRemaining(), hardGateTime.toString(), mapper.toResponses(shown));
    }
}
