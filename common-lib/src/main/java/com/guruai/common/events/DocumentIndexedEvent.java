package com.guruai.common.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kafka event — published by Document Service on topic {@code document.indexed}.
 *
 * <p>Fired after a document has been fully:
 * <ol>
 *   <li>Parsed by Apache Tika</li>
 *   <li>Chunked (1 000 chars / 150 overlap)</li>
 *   <li>Embedded via Gemini Embedding API</li>
 *   <li>Stored in pgvector</li>
 * </ol>
 *
 * <p>Consumers:
 * <ul>
 *   <li><b>Flashcard Service</b> — auto-generates SM-2 flashcards from chunks</li>
 *   <li><b>Knowledge Service</b> — registers extracted topics in the user profile</li>
 * </ul>
 *
 * @param eventId      unique event ID
 * @param documentId   UUID of the stored document row in document_db
 * @param sessionId    the study session this document was uploaded into
 * @param userId       owner of the session
 * @param filename     original filename (e.g. "chapter3.pdf")
 * @param chunkCount   total number of vector chunks stored
 * @param topics       auto-extracted subject topics (via Spring AI Structured Output)
 * @param subject      primary detected subject (e.g. "Data Structures")
 * @param occurredAt   when indexing completed
 */
public record DocumentIndexedEvent(
        String       eventId,
        String       documentId,
        String       sessionId,
        String       userId,
        String       filename,
        int          chunkCount,
        List<String> topics,
        String       subject,
        Instant      occurredAt
) {
    public static DocumentIndexedEvent of(
            String documentId, String sessionId, String userId,
            String filename, int chunkCount, List<String> topics, String subject) {
        return new DocumentIndexedEvent(
                UUID.randomUUID().toString(),
                documentId, sessionId, userId,
                filename, chunkCount, topics, subject,
                Instant.now()
        );
    }
}
