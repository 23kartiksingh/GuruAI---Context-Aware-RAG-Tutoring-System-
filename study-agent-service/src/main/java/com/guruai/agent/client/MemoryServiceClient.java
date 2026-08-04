package com.guruai.agent.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Calls user-memory-service to retrieve the user's learning preferences
 * as a formatted context block to inject into the system prompt.
 */
@Component
public class MemoryServiceClient {

    private final WebClient webClient;

    public MemoryServiceClient(
            @Value("${guruai.services.memory-service-url:http://user-memory-service:8088}") String baseUrl,
            WebClient.Builder builder) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Fetches the memory context string for a user.
     * Returns empty string if the memory service is unavailable.
     */
    public String getMemoryContext(String userId) {
        try {
            Map<?, ?> response = webClient.get()
                    .uri("/memory/{userId}/context", userId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response != null && response.get("data") != null) {
                return response.get("data").toString();
            }
        } catch (Exception ignored) {}
        return "";
    }
}
