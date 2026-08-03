package com.guruai.document.event.consumer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.SessionDeletedEvent;
import com.guruai.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Deletes a session's documents — and their pgvector chunks — when the
 * session is deleted.
 *
 * <p>Study Agent owns sessions but not documents, so rather than reaching
 * into this service's database it publishes {@code session.deleted} and this
 * service removes what it owns.
 *
 * <p>Naturally idempotent: replaying the event deletes nothing the second
 * time round.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SessionDeletedConsumer {

    private final DocumentService documentService;

    @KafkaListener(topics = KafkaTopics.SESSION_DELETED, groupId = "document-service-group")
    public void onSessionDeleted(SessionDeletedEvent event) {
        log.info("Received session.deleted userId={} sessionId={}", event.userId(), event.sessionId());
        try {
            documentService.deleteBySession(event.sessionId(), event.userId());
        } catch (Exception e) {
            log.error("Failed to delete documents for sessionId={}: {}",
                    event.sessionId(), e.getMessage(), e);
        }
    }
}
