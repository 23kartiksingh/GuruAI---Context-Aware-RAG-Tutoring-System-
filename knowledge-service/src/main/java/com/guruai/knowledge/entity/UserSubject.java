package com.guruai.knowledge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Records which subjects a user has enrolled in for GuruAI study tracking.
 */
@Entity
@Table(
    name = "user_subjects",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_session_subject",
        columnNames = {"user_id", "session_id", "subject"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class UserSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Session the enrolment came from, so it can be cleaned up with it. */
    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "enrolled_at", updatable = false, nullable = false)
    private Instant enrolledAt;

    @PrePersist
    void prePersist() {
        enrolledAt = Instant.now();
    }

    public UserSubject(UUID userId, UUID sessionId, String subject) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.subject = subject;
    }
}
