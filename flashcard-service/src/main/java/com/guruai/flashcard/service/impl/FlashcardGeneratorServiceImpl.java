package com.guruai.flashcard.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guruai.flashcard.entity.Flashcard;
import com.guruai.flashcard.service.FlashcardGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlashcardGeneratorServiceImpl implements FlashcardGeneratorService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are a flashcard generation engine for spaced repetition learning.
            Generate ONLY valid JSON — no markdown fences, no explanation.
            Output exactly this structure:
            {
              "flashcards": [
                {
                  "front": "question or prompt",
                  "back": "concise answer or explanation",
                  "topic": "specific topic"
                }
              ]
            }
            """;

    @Override
    public List<Flashcard> generateFromChunk(UUID userId, UUID sessionId,
                                              String subject, String topic,
                                              String chunkText, int count) {
        String userPrompt = String.format(
                "Generate exactly %d flashcards from this educational content.\n" +
                "Subject: %s | Topic: %s\n\nContent:\n%s",
                count, subject, topic, truncate(chunkText, 3000)
        );

        log.info("Generating {} flashcards for userId={} subject={}", count, userId, subject);

        String rawJson = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();

        return parseFlashcards(rawJson, userId, sessionId, subject, topic);
    }

    private List<Flashcard> parseFlashcards(String rawJson, UUID userId, UUID sessionId,
                                             String subject, String topic) {
        List<Flashcard> cards = new ArrayList<>();
        try {
            String cleaned = rawJson.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
            }
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode flashcards = root.get("flashcards");
            if (flashcards == null || !flashcards.isArray()) return cards;

            for (JsonNode fc : flashcards) {
                String cardTopic = fc.path("topic").asText(topic);
                cards.add(new Flashcard(userId, sessionId, subject, cardTopic,
                        fc.path("front").asText(), fc.path("back").asText()));
            }
        } catch (Exception e) {
            log.error("Failed to parse AI flashcard response: {}", e.getMessage());
        }
        return cards;
    }

    private String truncate(String text, int maxLen) {
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
