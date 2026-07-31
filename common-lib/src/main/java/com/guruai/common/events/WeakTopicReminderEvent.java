package com.guruai.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event — published by Knowledge Service's periodic weak-topic check,
 * on topic {@code weak.topic.reminder}.
 *
 * <p>Unlike {@link MasteryDroppedEvent} (a one-off alert fired the moment a
 * topic regresses to WEAK), this is a recurring proactive nudge: knowledge-
 * service scans for still-weak topics on a schedule and fires this for
 * students who haven't been reminded recently (cooldown enforced by the
 * scheduler via Redis, not by this event).
 *
 * <p>{@code sessionId} is always present — the scheduler only considers
 * topics that have one, since the whole point is a notification the student
 * can click through to the right session's chat.
 *
 * <p>Consumers:
 * <ul>
 *   <li><b>Notification Service</b> — pushes a "Let's revise [topic]" nudge
 *       that deep-links to {@code sessionId}.</li>
 * </ul>
 */
public record WeakTopicReminderEvent(
        String  eventId,
        String  userId,
        String  sessionId,
        String  subject,
        String  topic,
        double  emaScore,
        Instant occurredAt
) {
    public static WeakTopicReminderEvent of(
            String userId, String sessionId, String subject, String topic, double emaScore) {
        return new WeakTopicReminderEvent(
                UUID.randomUUID().toString(),
                userId, sessionId, subject, topic, emaScore,
                Instant.now()
        );
    }
}
