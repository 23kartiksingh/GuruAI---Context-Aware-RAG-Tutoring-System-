package com.guruai.flashcard.event.consumer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.DocumentIndexedEvent;
import com.guruai.flashcard.service.FlashcardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentIndexedConsumer {

    private final FlashcardService flashcardService;

    @KafkaListener(topics = KafkaTopics.DOCUMENT_INDEXED, groupId = "flashcard-service-group")
    public void onDocumentIndexed(DocumentIndexedEvent event) {
        log.info("Received document.indexed documentId={} userId={} subject={}",
                event.documentId(), event.userId(), event.subject());
        try {
            // Auto-generate flashcards for the first topic extracted from the document
            String topic = (event.topics() != null && !event.topics().isEmpty())
                    ? event.topics().get(0) : event.subject();
            // Use the document subject as placeholder chunk text prompt
            String chunkSummary = String.format(
                    "Document: %s | Subject: %s | Topics: %s",
                    event.filename(), event.subject(),
                    event.topics() != null ? String.join(", ", event.topics()) : "general");

            flashcardService.generateForSession(
                    UUID.fromString(event.userId()),
                    UUID.fromString(event.sessionId()),
                    event.subject(), topic, chunkSummary
            );
        } catch (Exception e) {
            log.error("Failed to generate flashcards for document {}: {}",
                    event.documentId(), e.getMessage(), e);
        }
    }
}
