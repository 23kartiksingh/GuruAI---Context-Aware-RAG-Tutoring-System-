package com.guruai.flashcard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SM-2 Spaced Repetition flashcard.
 *
 * <p>SM-2 Algorithm (SuperMemo):
 * <pre>
 *   EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))   where q = quality 0-5
 *   EF  >= 1.3 always (minimum ease factor)
 *   Interval:
 *     repetition == 0 → 1 day
 *     repetition == 1 → 6 days
 *     else            → ceil(previousInterval * EF)
 * </pre>
 */
@Entity
@Table(name = "flashcards")
@Getter
@Setter
@NoArgsConstructor
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "subject", length = 200)
    private String subject;

    @Column(name = "topic", length = 300)
    private String topic;

    @Column(name = "front", nullable = false, columnDefinition = "TEXT")
    private String front;

    @Column(name = "back", nullable = false, columnDefinition = "TEXT")
    private String back;

    // ── SM-2 state fields ─────────────────────────────────────
    /** Ease Factor — starts at 2.5, never drops below 1.3. */
    @Column(name = "ease_factor", nullable = false)
    private double easeFactor = 2.5;

    /** Inter-repetition interval in days. */
    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 1;

    /** Number of times this card has been reviewed. */
    @Column(name = "repetitions", nullable = false)
    private int repetitions = 0;

    /** Date when this card is next due for review. */
    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (nextReviewDate == null) {
            nextReviewDate = LocalDate.now();
        }
    }

    public Flashcard(UUID userId, UUID sessionId, String subject, String topic,
                     String front, String back) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.subject = subject;
        this.topic = topic;
        this.front = front;
        this.back = back;
    }
}
