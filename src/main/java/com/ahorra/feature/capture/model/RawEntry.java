package com.ahorra.feature.capture.model;

import com.ahorra.feature.capture.enums.CaptureStatus;
import com.ahorra.feature.capture.enums.EntrySource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * The user's spoken/typed input, stored exactly as received. Written before
 * any parsing happens so nothing the user said is ever lost, and kept as the
 * audit trail for whatever gets parsed from it.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "raw_entries")
public class RawEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private EntrySource source;

    @Column(name = "raw_text", nullable = false, length = 2000)
    private String rawText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CaptureStatus status = CaptureStatus.RECEIVED;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Optional client-supplied key making capture retries idempotent. */
    @Column(name = "client_request_id", length = 100)
    private String clientRequestId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
