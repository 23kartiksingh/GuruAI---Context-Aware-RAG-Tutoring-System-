package com.guruai.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event — published by Auth Service on topic {@code user.registered}.
 *
 * <p>Consumers:
 * <ul>
 *   <li><b>Knowledge Service</b> — initialises an empty mastery profile for the user</li>
 *   <li><b>User Memory Service</b> — creates an empty preferences store</li>
 *   <li><b>Notification Service</b> — sends a welcome notification</li>
 * </ul>
 *
 * @param eventId   unique ID for idempotency / deduplication
 * @param userId    UUID of the newly registered user
 * @param username  login name (lowercase, trimmed)
 * @param name      display name provided at registration
 * @param occurredAt wall-clock time when the registration was committed
 */
public record UserRegisteredEvent(
        String  eventId,
        String  userId,
        String  username,
        String  name,
        Instant occurredAt
) {
    /** Convenience factory — generates eventId and sets occurredAt to now. */
    public static UserRegisteredEvent of(String userId, String username, String name) {
        return new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                userId,
                username,
                name,
                Instant.now()
        );
    }
}
