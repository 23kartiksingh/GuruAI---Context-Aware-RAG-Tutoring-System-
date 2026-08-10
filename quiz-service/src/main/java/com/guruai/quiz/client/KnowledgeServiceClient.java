package com.guruai.quiz.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.guruai.common.enums.DifficultyLevel;
import com.guruai.common.enums.MasteryLevel;
import com.guruai.common.security.InternalAccessProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Reads the student's mastery from knowledge-service so quizzes can be
 * generated at a difficulty that matches what they actually know.
 *
 * <p>Uses {@link RestClient} rather than WebClient — quiz-service is a
 * plain servlet app with no reactive stack, and this is a single blocking
 * call made while generating a quiz.
 *
 * <p>Every request carries the shared internal secret, because downstream
 * services reject anything that didn't come through the gateway
 * ({@code InternalAccessFilter}).
 *
 * <p>Responses are read as {@link JsonNode}: knowledge-service owns those
 * response shapes, and copying its DTOs here would create a second
 * definition that quietly drifts.
 */
@Slf4j
@Component
public class KnowledgeServiceClient {

    private final RestClient restClient;

    public KnowledgeServiceClient(
            @Value("${guruai.services.knowledge-service-url:http://knowledge-service:8085}") String baseUrl,
            InternalAccessProperties internalProps) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Secret", internalProps.secret())
                .build();
    }

    /**
     * Pick a quiz difficulty from the student's current mastery — the "Auto"
     * option in the UI.
     *
     * <p>Prefers the specific topic being quizzed; falls back to the subject's
     * topics, then to the student's overall average. A brand-new student with
     * no quiz history has nothing to derive from, so they start at BEGINNER.
     *
     * @param topic may be null — the caller often only knows the subject
     * @return the resolved difficulty, never null
     */
    public DifficultyLevel resolveDifficulty(String userId, String subject, String topic) {
        try {
            JsonNode root = restClient.get()
                    .uri("/knowledge/{userId}/profile", userId)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode data = root == null ? null : root.get("data");
            JsonNode topics = data == null ? null : data.get("topics");
            if (topics == null || topics.isEmpty()) {
                log.info("Auto difficulty: no mastery history for userId={} — starting at BEGINNER", userId);
                return DifficultyLevel.BEGINNER;
            }

            // 1. Exact topic match, when a topic was requested.
            if (topic != null && !topic.isBlank()) {
                for (JsonNode t : topics) {
                    if (t.get("topic").asText().equalsIgnoreCase(topic.strip())) {
                        return fromNode(t, "topic '" + topic + "'", userId);
                    }
                }
            }

            // 2. Average across the subject's topics.
            double sum = 0;
            int count = 0;
            for (JsonNode t : topics) {
                if (t.get("subject").asText().equalsIgnoreCase(subject.strip())) {
                    sum += t.get("emaScore").asDouble();
                    count++;
                }
            }
            if (count > 0) {
                DifficultyLevel resolved = DifficultyLevel.from(MasteryLevel.fromScore(sum / count));
                log.info("Auto difficulty for userId={} subject='{}': {} (avg of {} topics)",
                        userId, subject, resolved, count);
                return resolved;
            }

            // 3. Nothing for this subject — use the overall average.
            double overall = data.get("overallMasteryPct").asDouble() / 100.0;
            DifficultyLevel resolved = DifficultyLevel.from(MasteryLevel.fromScore(overall));
            log.info("Auto difficulty for userId={}: {} (overall average, no data for subject '{}')",
                    userId, resolved, subject);
            return resolved;

        } catch (Exception e) {
            // Never fail quiz generation over this — an unavailable mastery
            // service just means we can't personalise the difficulty.
            log.warn("Auto difficulty lookup failed for userId={} ({}), defaulting to INTERMEDIATE",
                    userId, e.getMessage());
            return DifficultyLevel.INTERMEDIATE;
        }
    }

    private DifficultyLevel fromNode(JsonNode topicNode, String matchedOn, String userId) {
        MasteryLevel mastery = MasteryLevel.valueOf(topicNode.get("masteryLevel").asText());
        DifficultyLevel resolved = DifficultyLevel.from(mastery);
        log.info("Auto difficulty for userId={} matched {}: {} (mastery {})",
                userId, matchedOn, resolved, mastery);
        return resolved;
    }
}
