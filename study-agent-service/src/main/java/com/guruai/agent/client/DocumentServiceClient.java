package com.guruai.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calls document-service's hybrid search (Dense + BM25 + RRF over pgvector)
 * and returns the matching chunk texts for the CRAG pipeline.
 *
 * <p>Contract (must stay in sync with document-service's DocumentController):
 * {@code POST /documents/{sessionId}/search} with body
 * {@code {query, topK, userId}}, returning {@code ApiResponse<List<ChunkResponse>>}.
 * We only need each chunk's {@code content} here, so the response is read as
 * a JsonNode instead of duplicating document-service's ChunkResponse DTO.
 * (An earlier revision of this client called {@code POST /documents/search} —
 * a path that doesn't exist — and expected plain strings; every retrieval
 * 404'd and chat silently fell back to general knowledge.)
 */
@Slf4j
@Component
public class DocumentServiceClient {

    private final WebClient webClient;

    public DocumentServiceClient(
            @Value("${guruai.services.document-service-url:http://document-service:8082}") String baseUrl,
            WebClient.Builder builder) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Searches the vector store for chunks relevant to a query within a session.
     *
     * @param sessionId the session whose document embeddings to search
     * @param userId    the user making the request (SearchRequest requires it)
     * @param query     the user's question text
     * @param topK      number of top chunks to return
     * @return list of relevant chunk texts; empty if none found or on error
     *         (retrieval failure should degrade chat, not crash it)
     */
    public List<String> searchChunks(String sessionId, String userId, String query, int topK) {
        try {
            JsonNode root = webClient.post()
                    .uri("/documents/{sessionId}/search", sessionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "query", query,
                            "topK", topK,
                            "userId", userId
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

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
