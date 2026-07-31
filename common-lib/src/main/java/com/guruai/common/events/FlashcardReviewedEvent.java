package com.guruai.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event — published by Flashcard Service on topic {@code flashcard.reviewed}.
 *
 * <p>Fired after a user rates a flashcard review using the SM-2 quality scale (0–5).
 *
 * <p>Quality scale semantics:
 * <ul>
 *   <li>0–1 → Complete blackout / incorrect response</li>
 *   <li>2   → Incorrect, but correct felt familiar</li>
 *   <li>3   → Correct with serious difficulty</li>
 *   <li>4   → Correct with hesitation</li>
 *   <li>5   → Perfect response</li>
 * </ul>
 *
 * <p>Currently has no consumer. An earlier design had Knowledge Service
 * convert the quality score to a binary EMA signal ({@code quality >= 3} →
 * correct) and update topic mastery from it — that wiring was deliberately
 * removed (a flashcard self-rating isn't as reliable a "did they actually
 * understand this" signal as an explicit quiz answer), but this event is
 * still published on every review as an activity signal, the same pattern
 * {@code chat.message.saved} uses — available groundwork for a future
 * streak/engagement feature rather than mastery scoring.
 *
 * @param eventId    unique event ID
 * @param cardId     UUID of the flashcard
 * @param userId     the student who reviewed
 * @param sessionId  session the card belongs to — mastery is tracked per
 *                   session, so the consumer needs it to know which record
 *                   this review should move
 * @param topic      topic the flashcard covers
 * @param subject    subject it belongs to
 * @param quality    SM-2 quality score (0–5)
 * @param occurredAt when the review was submitted
 */
public record FlashcardReviewedEvent(
        String  eventId,
        String  cardId,
        String  userId,
        String  sessionId,
        String  topic,
        String  subject,
        int     quality,
        Instant occurredAt
) {
    /** @return {@code true} if quality score indicates a correct recall (≥ 3) */
    public boolean isCorrect() {
        return quality >= 3;
    }

    public static FlashcardReviewedEvent of(
            String cardId, String userId, String sessionId,
            String topic, String subject, int quality) {
        return new FlashcardReviewedEvent(
                UUID.randomUUID().toString(),
                cardId, userId, sessionId, topic, subject, quality,
                Instant.now()
        );
    }
}
