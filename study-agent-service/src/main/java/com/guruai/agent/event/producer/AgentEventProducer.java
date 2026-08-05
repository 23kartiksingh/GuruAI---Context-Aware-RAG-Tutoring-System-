package com.guruai.agent.event.producer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.ChatMessageSavedEvent;
import com.guruai.common.events.SessionDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@code chat.message.saved} — fired once per persisted message
 * (so a normal chat turn produces two events: the user's message and the
 * assistant's reply).
 *
 * <p>Main consumer is knowledge-service, which mines chat activity as an
 * extra mastery signal alongside quiz results and flashcard reviews.
 *
 * <p>Keyed by userId so all of one user's events land on the same partition
 * (Kafka only guarantees ordering within a partition, and mastery updates
 * for a user should be applied in order).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishChatMessageSaved(ChatMessageSavedEvent event) {
        kafkaTemplate.send(KafkaTopics.CHAT_MESSAGE_SAVED, event.userId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // Log and move on — a missed mastery signal shouldn't
                        // fail the chat request the user is waiting on.
                        log.error("Failed to publish chat.message.saved for messageId={}: {}",
                                event.messageId(), ex.getMessage());
                    } else {
                        log.debug("Published chat.message.saved messageId={} role={}",
                                event.messageId(), event.role());
                    }
                });
    }

    /**
     * Announce that a session was deleted so document-, flashcard- and
     * knowledge-service can drop the data they hold for it.
     *
     * <p>Unlike the chat event above, a failure here matters: it means the
     * session is gone but its documents, cards and mastery are orphaned. It's
     * logged as an error for that reason, though the delete itself still
     * succeeds — the alternative (rolling back a user-requested deletion
     * because a broker was briefly unreachable) is worse.
     */
    public void publishSessionDeleted(SessionDeletedEvent event) {
        kafkaTemplate.send(KafkaTopics.SESSION_DELETED, event.userId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish session.deleted for sessionId={} — downstream "
                                        + "documents/flashcards/mastery may be orphaned: {}",
                                event.sessionId(), ex.getMessage());
                    } else {
                        log.info("Published session.deleted sessionId={}", event.sessionId());
                    }
                });
    }
}
