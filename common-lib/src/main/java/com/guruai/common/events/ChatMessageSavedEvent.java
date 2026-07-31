package com.guruai.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event — published by Chat Service on topic {@code chat.message.saved}.
 *
 * <p>Fired every time a message (user or assistant) is persisted to chat_db.
 *
 * <p>Consumers:
 * <ul>
 *   <li><b>Knowledge Service</b> — may extract topic context from the message content
 *       to update EMA scores when the user explicitly marks answers as correct/wrong
 *       in chat (bonus signal on top of quiz answers).</li>
 * </ul>
 *
 * @param eventId    unique event ID
 * @param messageId  UUID of the saved message row
 * @param sessionId  the session the message belongs to
 * @param userId     the user who owns the session
 * @param role       {@code "user"} | {@code "assistant"} | {@code "quiz"}
 * @param content    full text of the message (may be large for assistant responses)
 * @param occurredAt timestamp of persistence
 */
public record ChatMessageSavedEvent(
        String  eventId,
        String  messageId,
        String  sessionId,
        String  userId,
        String  role,
        String  content,
        Instant occurredAt
) {
    public static ChatMessageSavedEvent of(
            String messageId, String sessionId, String userId,
            String role, String content) {
        return new ChatMessageSavedEvent(
                UUID.randomUUID().toString(),
                messageId, sessionId, userId, role, content,
                Instant.now()
        );
    }
}
