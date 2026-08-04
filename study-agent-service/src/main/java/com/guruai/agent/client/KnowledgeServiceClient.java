package com.guruai.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Calls knowledge-service for the user's mastery data. Used two ways:
 * <ul>
 *   <li>by {@link com.guruai.agent.tool.AgentTools} when the LLM decides
 *       mid-conversation that it needs mastery info (tool calling), and</li>
 *   <li>by the learning-path generator, which builds a study plan around
 *       the user's weak topics.</li>
 * </ul>
 *
 * <p>Responses are parsed as {@link JsonNode} instead of binding to typed
 * DTOs — knowledge-service owns those response shapes, and duplicating its
 * DTO classes here just to deserialize a couple of fields would create a
 * second copy that silently drifts out of date.
 *
 * <p>All methods degrade gracefully (empty result on failure) — mastery data
 * makes answers better but is never worth failing a chat request over.
 */
@Slf4j
@Component
public class KnowledgeServiceClient {

    private final WebClient webClient;

    public KnowledgeServiceClient(
            @Value("${guruai.services.knowledge-service-url:http://knowledge-service:8085}") String baseUrl,
            WebClient.Builder builder) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Human-readable summary of the user's full mastery profile,
     * e.g. {@code "Binary Trees (Data Structures): 82% [STRONG]"} per line.
     * Formatted here (not raw JSON) because it goes directly into an LLM
     * prompt via tool calling.
     */
    // Bounds the tool result — without a cap this grows every time the
    // student is assessed on a new topic, and being a tool result it gets
    // resent to Groq on every subsequent tool-calling round-trip within the
    // same turn, not just once. 15 topics is enough to be genuinely useful
    // for "what should I study" without becoming an unbounded payload.
    private static final int MAX_PROFILE_TOPICS = 15;

    public String getMasteryProfileSummary(String userId) {
        try {
            JsonNode root = webClient.get()
                    .uri("/knowledge/{userId}/profile", userId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            JsonNode data = root == null ? null : root.get("data");
            if (data == null || data.get("topics") == null || data.get("topics").isEmpty()) {
                return "No mastery data recorded yet for this student.";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Overall mastery: ")
              .append(data.get("overallMasteryPct").asDouble())
              .append("% across ").append(data.get("totalTopics").asInt()).append(" topics\n");
            int shown = 0;
            int total = data.get("topics").size();
            for (JsonNode t : data.get("topics")) {
                if (shown >= MAX_PROFILE_TOPICS) {
                    sb.append("- ... and ").append(total - shown).append(" more topics not shown\n");
                    break;
                }
                sb.append("- ").append(t.get("topic").asText())
                  .append(" (").append(t.get("subject").asText()).append("): ")
                  .append(Math.round(t.get("emaScore").asDouble() * 100)).append("% [")
                  .append(t.get("masteryLevel").asText()).append("]\n");
                shown++;
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("knowledge-service profile lookup failed for userId={}: {}", userId, e.getMessage());
            return "Mastery data is currently unavailable.";
        }
    }

    /**
     * The student's mastery relevant to one question.
     *
     * @param matchedTopic the topic name that matched the question, or null if
     *                     the figure is the student's overall average instead
     * @param level        WEAK / AVERAGE / STRONG
     * @param pct          mastery as a whole percentage
     */
    public record MasteryHint(String matchedTopic, String level, int pct) {}

    /**
     * Work out how well the student knows whatever they just asked about, so
     * the tutor can pitch its answer accordingly.
     *
     * <p>Matching is deliberately simple: a tracked topic counts as "the topic
     * being asked about" if its significant words all appear in the question
     * (e.g. "Cricket Dismissal Methods" matches "how do dismissal methods work
     * in cricket?"). The longest such match wins, since a more specific topic
     * is the better signal. With no match we fall back to the overall average —
     * still a useful hint about the student's general level — and with no
     * mastery data at all we return null and the caller uses a neutral default.
     *
     * <p>This is intentionally not an LLM call: it runs on every chat turn, and
     * spending a model round-trip just to pick a difficulty band would add
     * latency and cost to something a string comparison answers well enough.
     */
    public MasteryHint findMasteryForQuestion(String userId, String question) {
        try {
            JsonNode root = webClient.get()
                    .uri("/knowledge/{userId}/profile", userId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            JsonNode data = root == null ? null : root.get("data");
            if (data == null || data.get("topics") == null || data.get("topics").isEmpty()) {
                return null;
            }

            String haystack = question.toLowerCase();
            JsonNode best = null;
            int bestLength = -1;

            for (JsonNode t : data.get("topics")) {
                String topic = t.get("topic").asText();
                if (matchesQuestion(topic, haystack) && topic.length() > bestLength) {
                    best = t;
                    bestLength = topic.length();
                }
            }

            if (best != null) {
                return new MasteryHint(
                        best.get("topic").asText(),
                        best.get("masteryLevel").asText(),
                        (int) Math.round(best.get("emaScore").asDouble() * 100));
            }

            // No topic matched — fall back to the overall average.
            int overallPct = (int) Math.round(data.get("overallMasteryPct").asDouble());
            return new MasteryHint(null, levelFromPct(overallPct), overallPct);
        } catch (Exception e) {
            log.warn("knowledge-service mastery hint lookup failed for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    /** True when every significant word of the topic appears in the question. */
    private boolean matchesQuestion(String topic, String lowercaseQuestion) {
        String[] words = topic.toLowerCase().split("[^a-z0-9]+");
        boolean sawSignificantWord = false;
        for (String word : words) {
            // Skip short filler ("of", "in", "the") — requiring those to appear
            // would make almost nothing match.
            if (word.length() < 4) {
                continue;
            }
            sawSignificantWord = true;
            if (!lowercaseQuestion.contains(word)) {
                return false;
            }
        }
        return sawSignificantWord;
    }

    /**
     * Band the overall average using the same cut-offs as
     * {@link com.guruai.common.enums.MasteryLevel} — WEAK below 0.50,
     * STRONG above 0.75, AVERAGE in between.
     *
     * <p>(Note: KnowledgeController's javadoc quotes 0.4 / 0.7 for its
     * weak-topics and strong-topics endpoints. The enum is the source of
     * truth and those comments are stale.)
     */
    private String levelFromPct(int pct) {
        if (pct < 50) return "WEAK";
        if (pct > 75) return "STRONG";
        return "AVERAGE";
    }

    /**
     * The user's weak topics (EMA below the weak threshold) as
     * {@code "topic (subject): score%"} strings — the raw material for
     * learning-path generation.
     */
    public List<String> getWeakTopics(String userId) {
        try {
            JsonNode root = webClient.get()
                    .uri("/knowledge/{userId}/weak-topics", userId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            JsonNode data = root == null ? null : root.get("data");
            List<String> topics = new ArrayList<>();
            if (data != null) {
                for (JsonNode t : data) {
                    topics.add(t.get("topic").asText()
                            + " (" + t.get("subject").asText() + "): "
                            + Math.round(t.get("emaScore").asDouble() * 100) + "%");
                }
            }
            return topics;
        } catch (Exception e) {
            log.warn("knowledge-service weak-topics lookup failed for userId={}: {}", userId, e.getMessage());
            return List.of();
        }
    }
}
