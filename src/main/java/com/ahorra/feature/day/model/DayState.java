package com.ahorra.feature.day.model;

import com.ahorra.feature.day.enums.DayStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The recorded outcome of one user's calendar day. A day that was suspended
 * and later cleared keeps its suspendedAt, which is what marks its eventual
 * close as retroactive.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "day_states")
public class DayState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "day_date", nullable = false)
    private LocalDate dayDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DayStatus status;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
