package com.guruai.knowledge.event.consumer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.SessionDeletedEvent;
import com.guruai.knowledge.service.MasteryService;
import com.guruai.knowledge.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Drops the knowledge a deleted session produced.
 *
 * <p>Mastery is scoped to the session it was earned in, so removing a session
 * removes exactly its EMA records and subject enrolments — mastery built in
 * other sessions is untouched.
 *
 * <p>Deleting by (userId, sessionId) rather than sessionId alone is
 * deliberate: it means a malformed or spoofed event can only ever affect the
 * user it names.
 *
 * <p>Naturally idempotent — a redelivered event deletes zero rows the second
 * time, so no event-ID bookkeeping is needed here.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SessionDeletedConsumer {

    private final MasteryService masteryService;
    private final SubjectService subjectService;

    @KafkaListener(topics = KafkaTopics.SESSION_DELETED, groupId = "knowledge-service-group")
    public void onSessionDeleted(SessionDeletedEvent event) {
        log.info("Received session.deleted userId={} sessionId={}", event.userId(), event.sessionId());
        try {
            UUID userId = UUID.fromString(event.userId());
            UUID sessionId = UUID.fromString(event.sessionId());
            masteryService.deleteBySession(userId, sessionId);
            subjectService.deleteBySession(userId, sessionId);
        } catch (Exception e) {
            log.error("Failed to clean up knowledge for sessionId={}: {}",
                    event.sessionId(), e.getMessage(), e);
        }
    }
}
