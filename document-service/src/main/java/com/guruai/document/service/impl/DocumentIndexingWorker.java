package com.guruai.document.service.impl;

import com.guruai.document.entity.UserDocument;
import com.guruai.document.event.producer.DocumentEventProducer;
import com.guruai.document.repository.DocumentRepository;
import com.guruai.document.service.ChunkingService;
import com.guruai.document.util.TikaParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * The async half of the upload pipeline: Tika parse → chunk → topic
 * extraction (Gemini) → embed + store in pgvector → mark INDEXED →
 * publish {@code document.indexed}.
 *
 * <p>Why a separate bean instead of an {@code @Async} method inside
 * DocumentServiceImpl (where this code originally lived): Spring applies
 * {@code @Async} (and {@code @Transactional}) through a proxy, and a
 * this.method() self-invocation goes straight to the real object —
 * bypassing the proxy, so the annotation silently does nothing. The old
 * version therefore ran the whole parse/embed pipeline synchronously
 * inside the upload request, blocking the HTTP response it claimed to
 * return "immediately". Calling across bean boundaries (DocumentServiceImpl
 * → this worker) goes through the proxy, so @Async actually applies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIndexingWorker {

    private final DocumentRepository    documentRepository;
    private final ChunkingService       chunkingService;
    private final VectorStore           vectorStore;
    private final TikaParser            tikaParser;
    private final ChatClient            chatClient;
    private final DocumentEventProducer eventProducer;

    /** Structured-output target for topic extraction. */
    public record TopicExtractionResult(List<String> topics, String subject) {}

    @Async
    @Transactional
    public void processDocument(UUID docId, byte[] fileBytes,
                                String filename, String sessionId) {
        UserDocument doc = documentRepository.findById(docId).orElseThrow();
        try {
            // 1. Parse with Tika
            String text;
            try (var stream = new java.io.ByteArrayInputStream(fileBytes)) {
                text = tikaParser.parse(stream, filename);
            }

            if (text.isBlank()) {
                doc.markFailed("No text could be extracted from the document.");
                documentRepository.save(doc);
                return;
            }
            log.debug("Parsed {} chars from '{}'", text.length(), filename);

            // 2. Chunk text
            List<String> chunks = chunkingService.chunk(text);
            log.debug("Generated {} chunks from '{}'", chunks.size(), filename);

            // 3. Extract topics and subject (Gemini, structured output)
            TopicExtractionResult topicResult = extractTopics(text, filename);

            // 4. Build Spring AI Document objects with metadata
            List<Document> aiDocs = IntStream.range(0, chunks.size())
                    .mapToObj(i -> new Document(chunks.get(i), Map.of(
                            "document_id",  docId.toString(),
                            "session_id",   sessionId,
                            "user_id",      doc.getUserId().toString(),
                            "filename",     filename,
                            "chunk_index",  i,
                            "subject",      Objects.requireNonNullElse(topicResult.subject(), ""),
                            "topics",       String.join(",", topicResult.topics())
                    )))
                    .toList();

            // 5. Embed and store in pgvector (Spring AI embeds internally)
            vectorStore.accept(aiDocs);
            log.info("Embedded and stored {} chunks for documentId={}", aiDocs.size(), docId);

            // 6. Mark as INDEXED
            doc.markIndexed(chunks.size(), topicResult.topics(), topicResult.subject());
            documentRepository.save(doc);

            // 7. Publish Kafka event
            eventProducer.publishDocumentIndexed(
                    docId.toString(), sessionId, doc.getUserId().toString(),
                    filename, chunks.size(), topicResult.topics(), topicResult.subject()
            );

        } catch (TikaException | IOException e) {
            log.error("Failed to process document {}: {}", docId, e.getMessage(), e);
            doc.markFailed("Parse error: " + e.getMessage());
            documentRepository.save(doc);
        } catch (Exception e) {
            log.error("Unexpected error processing document {}", docId, e);
            doc.markFailed("Unexpected error: " + e.getMessage());
            documentRepository.save(doc);
        }
    }

    /**
     * Use Spring AI ChatClient with Structured Output to extract academic
     * topics and a primary subject from the document's text. Only the first
     * 3 000 characters go to the model — enough to identify the subject
     * without burning tokens on the whole document.
     */
    private TopicExtractionResult extractTopics(String text, String filename) {
        String preview = text.length() > 3000 ? text.substring(0, 3000) : text;

        String systemPrompt = """
                You are an academic content analyser.
                Given a document excerpt, extract:
                1. A list of 3–7 specific academic topics (e.g. "Binary Trees", "DFS", "Recursion")
                2. One primary subject area (e.g. "Data Structures", "Calculus", "World History")

                Respond in JSON with this exact schema:
                {"topics": ["topic1", "topic2", ...], "subject": "Subject Name"}
                Only return valid JSON. No preamble.
                """;

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user("Document filename: " + filename + "\n\nExcerpt:\n" + preview)
                    .call()
                    .entity(TopicExtractionResult.class);
        } catch (Exception e) {
            log.warn("Topic extraction failed for '{}': {} — using empty topics", filename, e.getMessage());
            return new TopicExtractionResult(List.of(), null);
        }
    }
}
