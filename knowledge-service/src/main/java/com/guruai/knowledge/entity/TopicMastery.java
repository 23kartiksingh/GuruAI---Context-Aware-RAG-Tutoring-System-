package com.guruai.knowledge.entity;

import com.guruai.common.enums.MasteryLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks a student's mastery level for a specific topic within a subject.
 * Uses Exponential Moving Average (EMA) to smooth mastery score over time.
 *
 * <p>EMA formula: newScore = alpha * signal + (1 - alpha) * oldScore
 * where signal=1.0 for correct, 0.0 for incorrect, alpha=0.3 (from Python tracker.py)
 */
@Entity
@Table(
    name = "topic_mastery",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_session_subject_topic",
        columnNames = {"user_id", "session_id", "subject", "topic"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class TopicMastery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Study session this mastery was built in — the same topic studied in two
     * sessions tracks two independent scores, and deleting a session removes
     * exactly its rows. Nullable only for rows created before mastery became
     * session-scoped (see V2 migration).
     */
    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "topic", nullable = false, length = 300)
    private String topic;

    /** EMA score: 0.0 (worst) → 1.0 (best). Initialised at 0.5 (neutral). */
    @Column(name = "ema_score", nullable = false)
    private double emaScore = 0.5;

    @Column(name = "correct_count", nullable = false)
    private int correctCount = 0;

    @Column(name = "total_count", nullable = false)
    private int totalCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "mastery_level", nullable = false, length = 20)
    private MasteryLevel masteryLevel = MasteryLevel.AVERAGE;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        lastUpdated = createdAt;
        masteryLevel = MasteryLevel.AVERAGE;
    }

    @PreUpdate
    void preUpdate() {
        lastUpdated = Instant.now();
    }

    /**
     * Applies one EMA update step.
     *
     * @param correct whether the answer/review was correct
     * @param alpha   smoothing factor (0 &lt; alpha &lt; 1)
     * @param weakThreshold   score below this → WEAK
     * @param strongThreshold score at or above this → STRONG
     */
    public void applyEmaUpdate(boolean correct, double alpha,
                               double weakThreshold, double strongThreshold) {
        double signal = correct ? 1.0 : 0.0;
        emaScore = alpha * signal + (1.0 - alpha) * emaScore;
        // clamp to [0,1]
        emaScore = Math.max(0.0, Math.min(1.0, emaScore));

        totalCount++;
        if (correct) correctCount++;

        // Classify mastery level
        if (emaScore < weakThreshold) {
            masteryLevel = MasteryLevel.WEAK;
        } else if (emaScore >= strongThreshold) {
            masteryLevel = MasteryLevel.STRONG;
        } else {
            masteryLevel = MasteryLevel.AVERAGE;
        }
    }

    /** Convenience constructor (used in MasteryServiceImpl). */
    public TopicMastery(UUID userId, UUID sessionId, String subject, String topic) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.subject = subject;
        this.topic = topic;
    }
}
