package com.guruai.common.events;

import com.guruai.common.enums.MasteryLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event — published by Knowledge Service on topic {@code mastery.dropped}.
 *
 * <p>Fired when a topic's EMA score crosses downward from AVERAGE → WEAK
 * (score drops below 0.5). This acts as an alerting signal.
 *
 * <p>Consumers:
 * <ul>
 *   <li><b>Notification Service</b> — pushes a "Your mastery of [topic] has dropped"
 *       alert to the user via WebSocket.</li>
 * </ul>
 *
 * @param eventId       unique event ID
 * @param userId        the student whose mastery dropped
 * @param subject       subject containing the affected topic
 * @param topic         the specific topic whose EMA score dropped
 * @param previousLevel the level the topic was at before this event
 * @param newEmaScore   the new (lower) EMA score
 * @param occurredAt    when the drop was detected
 */
public record MasteryDroppedEvent(
        String       eventId,
        String       userId,
        String       subject,
        String       topic,
        MasteryLevel previousLevel,
        double       newEmaScore,
        Instant      occurredAt
) {
    public static MasteryDroppedEvent of(
            String userId, String subject, String topic,
            MasteryLevel previousLevel, double newEmaScore) {
        return new MasteryDroppedEvent(
                UUID.randomUUID().toString(),
                userId, subject, topic,
                previousLevel, newEmaScore,
                Instant.now()
        );
    }
}
