package com.guruai.quiz.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.guruai.common.security.InternalAccessProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calls document-service's hybrid search so quiz generation can be grounded
 * in the student's actual uploaded documents instead of the model's general
 * knowledge of a bare topic name — the same idea as study-agent-service's
 * CRAG pipeline, just for quiz questions instead of chat answers.
 *
 * <p>Uses {@link RestClient} rather than WebClient for the same reason as
 * {@link KnowledgeServiceClient} — quiz-service is a plain servlet app, and
 * this is one blocking call made while generating a quiz.
 *
 * <p>Contract (must stay in sync with document-service's DocumentController):
 * {@code POST /documents/{sessionId}/search} with body
 * {@code {query, topK, userId}}, returning {@code ApiResponse<List<ChunkResponse>>}.
 * See study-agent-service's DocumentServiceClient for the same contract.
 */
@Slf4j
@Component
public class DocumentServiceClient {

    private final RestClient restClient;

    public DocumentServiceClient(
            @Value("${guruai.services.document-service-url:http://document-service:8082}") String baseUrl,
            InternalAccessProperties internalProps) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Secret", internalProps.secret())
                .build();
    }

    /**
     * Searches the session's document chunks for a query (typically the quiz
     * topic). Degrades to an empty list on failure or when nothing matches —
     * the caller falls back to topic-name-only generation rather than failing
     * the whole quiz over a retrieval miss.
     */
    public List<String> searchChunks(String sessionId, String userId, String query, int topK) {
        try {
            JsonNode root = restClient.post()
                    .uri("/documents/{sessionId}/search", sessionId)
                    .body(Map.of(
                            "query", query,
                            "topK", topK,
                            "userId", userId
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            List<String> chunks = new ArrayList<>();
            JsonNode data = root == null ? null : root.get("data");
            if (data != null && data.isArray()) {
                for (JsonNode chunk : data) {
                    JsonNode content = chunk.get("content");
                    if (content != null && !content.asText().isBlank()) {
                        chunks.add(content.asText());
                    }
                }
            }
            return chunks;
        } catch (Exception e) {
            log.warn("Document search failed for sessionId={}: {}", sessionId, e.getMessage());
            return List.of();
        }
    }
}
