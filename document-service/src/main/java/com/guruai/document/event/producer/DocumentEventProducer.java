package com.guruai.document.event.producer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.DocumentIndexedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publishes document-domain Kafka events.
 *
 * <p>Currently publishes:
 * <ul>
 *   <li>{@code document.indexed} — when all chunks are embedded and stored successfully</li>
 * </ul>
 *
 * <p>Consumers of {@code document.indexed}:
 * <ul>
 *   <li><b>Learning Path Service</b> — detect new topics, update learning path suggestions</li>
 *   <li><b>Study Agent Service</b> — pre-warm retriever cache for common queries</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish a {@link DocumentIndexedEvent} to the {@code document.indexed} Kafka topic.
     *
     * <p>Message key = {@code documentId} for per-document ordering guarantee.
     *
     * @param documentId UUID of the indexed document
     * @param sessionId  study session that received the document
     * @param userId     user who uploaded the document
     * @param filename   original file name
     * @param chunkCount number of chunks stored in pgvector
     * @param topics     AI-extracted topic list
     * @param subject    primary detected subject
     */
    @Async
    public void publishDocumentIndexed(String documentId, String sessionId, String userId,
                                        String filename, int chunkCount,
                                        List<String> topics, String subject) {
        DocumentIndexedEvent event = DocumentIndexedEvent.of(
                documentId, sessionId, userId, filename, chunkCount, topics, subject);

        kafkaTemplate.send(KafkaTopics.DOCUMENT_INDEXED, documentId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish DocumentIndexedEvent for documentId={}: {}",
                                  documentId, ex.getMessage());
                    } else {
                        log.info("Published DocumentIndexedEvent: documentId={}, topics={}",
                                 documentId, topics);
                    }
                });
    }
}
