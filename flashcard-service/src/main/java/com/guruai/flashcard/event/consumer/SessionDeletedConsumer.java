package com.guruai.flashcard.event.consumer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.SessionDeletedEvent;
import com.guruai.flashcard.service.FlashcardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Deletes the flashcards generated in a session when that session is deleted.
 *
 * <p>Cards are auto-generated from the session's documents, so once the
 * session and its documents are gone the cards have nothing left to point at.
 *
 * <p>Naturally idempotent — a redelivered event finds nothing left to delete.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SessionDeletedConsumer {

    private final FlashcardService flashcardService;

    @KafkaListener(topics = KafkaTopics.SESSION_DELETED, groupId = "flashcard-service-group")
    public void onSessionDeleted(SessionDeletedEvent event) {
        log.info("Received session.deleted userId={} sessionId={}", event.userId(), event.sessionId());
        try {
            flashcardService.deleteBySession(
                    UUID.fromString(event.userId()),
                    UUID.fromString(event.sessionId()));
        } catch (Exception e) {
            log.error("Failed to delete flashcards for sessionId={}: {}",
                    event.sessionId(), e.getMessage(), e);
        }
    }
}
