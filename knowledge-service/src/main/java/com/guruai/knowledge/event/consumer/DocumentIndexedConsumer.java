package com.guruai.knowledge.event.consumer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.DocumentIndexedEvent;
import com.guruai.knowledge.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * When document-service finishes indexing an upload, it publishes the
 * subject + topics it auto-detected from the content. We use that to enrol
 * the user in the subject automatically — so quiz results on that material
 * later land under a subject that already exists in their profile, instead
 * of the user having to add "Operating Systems" by hand before their OS
 * textbook counts toward anything.
 *
 * <p>Deliberately NOT seeded here: TopicMastery rows for the extracted
 * topics. Mastery rows are created lazily on the first real assessment
 * signal (quiz answer / flashcard review — see MasteryServiceImpl), because
 * a document merely containing a topic says nothing about how well the
 * student knows it, and pre-seeding rows at a neutral score would pollute
 * weak/strong topic lists with topics the student was never tested on.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentIndexedConsumer {

    private final SubjectService subjectService;

    @KafkaListener(topics = KafkaTopics.DOCUMENT_INDEXED, groupId = "knowledge-service-group")
    public void onDocumentIndexed(DocumentIndexedEvent event) {
        log.info("Received document.indexed userId={} file='{}' subject='{}' topics={}",
                event.userId(), event.filename(), event.subject(), event.topics());
        try {
            if (event.subject() != null && !event.subject().isBlank()) {
                // addSubject is idempotent (existence check inside), so
                // re-indexing or duplicate events are harmless.
                subjectService.addSubject(
                        UUID.fromString(event.userId()),
                        event.sessionId() == null ? null : UUID.fromString(event.sessionId()),
                        event.subject());
            }
        } catch (Exception e) {
            log.error("Failed to process document.indexed for userId={}: {}",
                    event.userId(), e.getMessage(), e);
        }
    }
}
