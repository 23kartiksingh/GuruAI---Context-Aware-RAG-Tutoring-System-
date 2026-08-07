package com.guruai.knowledge.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Classifies a raw topic name into a canonical one so mastery doesn't
 * fragment into many near-duplicate topics — e.g. "how do I join two
 * tables?" and "SQL query practice" should both land under "DBMS" rather
 * than becoming two (or more) separate tracked topics.
 *
 * <p>Called only when {@link com.guruai.knowledge.util.FuzzyTopicMatcher}
 * finds nothing close by string similarity — cheap typo/casing/plural
 * differences are handled there for free; this exists for topics that are
 * genuinely worded differently but belong under the same subject-matter
 * category, which needs actual understanding of the words, not string
 * distance.
 *
 * <p>Quiz completions happen a handful of times per session, not once per
 * chat message, so a small classification call here doesn't meaningfully
 * add to Groq's shared free-tier token budget the way a per-turn call
 * would.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopicCanonicalizerService {

    // Bounds the prompt size — a student's topic list only grows, and this
    // gets resent on every classification call.
    private static final int MAX_EXISTING_TOPICS = 20;

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You cumulate topic names for a student mastery tracker so near-duplicate
            or overly narrow topics don't fragment into separate records.

            Given a NEW topic and the student's EXISTING topics for this subject,
            respond with ONLY a topic name on a single line — no explanation, no quotes.

            - If the new topic is really the same subject-matter as one of the
              EXISTING topics (even worded completely differently), reply with
              that EXACT existing name, character for character.
            - Otherwise, reply with a short, standard, widely-recognised category
              name for the new topic. Examples: a question about table joins or
              SQL queries -> "DBMS"; a competitive-programming problem -> "CP";
              a question about pointers, recursion, or arrays -> "DSA"; a
              question about inheritance or polymorphism -> "OOPs". Prefer the
              shortest standard name a student would recognise over a long
              descriptive phrase.
            """;

    public String canonicalize(String rawTopic, String subject, List<String> existingTopics) {
        if (rawTopic == null || rawTopic.isBlank()) {
            return rawTopic;
        }
        try {
            List<String> capped = existingTopics.stream().distinct().limit(MAX_EXISTING_TOPICS).toList();
            String userPrompt = String.format(
                    "Subject: %s%nNew topic: %s%nExisting topics: %s",
                    subject, rawTopic, capped.isEmpty() ? "(none yet)" : String.join(", ", capped));

            String result = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            String cleaned = result == null ? "" : result.strip()
                    .replaceAll("^[\"']|[\"']$", "")
                    .lines().findFirst().orElse("").strip();

            if (cleaned.isEmpty()) {
                return rawTopic;
            }
            log.info("Canonicalized topic '{}' -> '{}'", rawTopic, cleaned);
            return cleaned;
        } catch (Exception e) {
            // Never fail mastery tracking over a naming call — an unavailable
            // or mis-parsed classification just means the raw topic gets used.
            log.warn("Topic canonicalization failed for '{}' ({}), keeping raw name",
                    rawTopic, e.getMessage());
            return rawTopic;
        }
    }
}
