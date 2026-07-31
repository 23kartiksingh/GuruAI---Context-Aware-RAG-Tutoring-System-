package com.guruai.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event — published by Study Agent Service on topic {@code session.deleted}.
 *
 * <p>Fired when a user deletes a study session. Study Agent removes the session
 * and its chat messages itself; everything else a session accumulated lives in
 * other services, and each cleans up its own data on this event rather than
 * Study Agent reaching across service boundaries to delete rows it doesn't own.
 *
 * <p>Consumers:
 * <ul>
 *   <li><b>Document Service</b> — deletes the session's documents and their
 *       pgvector chunks</li>
 *   <li><b>Flashcard Service</b> — deletes flashcards generated in the session</li>
 *   <li><b>Knowledge Service</b> — deletes mastery and subject enrolments
 *       recorded in the session</li>
 * </ul>
 *
 * <p>Cleanup is therefore eventually consistent: the session disappears from
 * the UI immediately, while the downstream deletions land a moment later.
 *
 * @param eventId    unique event ID (idempotency key for consumers)
 * @param sessionId  the deleted session
 * @param userId     owner of the session — consumers use it to scope deletes
 *                   so a malformed event can't wipe another user's data
 * @param occurredAt when the session was deleted
 */
public record SessionDeletedEvent(
        String  eventId,
        String  sessionId,
        String  userId,
        Instant occurredAt
) {
    public static SessionDeletedEvent of(String sessionId, String userId) {
        return new SessionDeletedEvent(
                UUID.randomUUID().toString(),
                sessionId, userId,
                Instant.now()
        );
    }
}
