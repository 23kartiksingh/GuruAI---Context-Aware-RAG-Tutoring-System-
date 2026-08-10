package com.guruai.memory.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guruai.memory.service.PreferenceExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PreferenceExtractorServiceImpl implements PreferenceExtractorService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are a preference extraction engine. Analyze the user's message and extract
            any learning preferences, study habits, personal interests, or personal context
            that would help an AI tutor personalize its responses and examples.

            Rules:
            - Extract ONLY factual preferences (not questions, not greetings).
            - Each preference must be a concise statement under 15 words.
            - CRITICAL: preserve every specific named detail EXACTLY as the user wrote it —
              names of shows, games, books, languages, tools, teams, etc. Never generalize a
              specific name away into a vaguer category. "my favorite anime is Hunter x
              Hunter" must become a preference that still says "Hunter x Hunter" by name, NOT
              a generic "likes anime" — the specific name is the whole point, it's what makes
              a later example personal instead of generic.
            - Return ONLY valid JSON. No markdown, no explanation.
            - Format: {"preferences": ["preference 1", "preference 2"]}
            - If no preferences found, return: {"preferences": []}

            Example:
            User message: "I'm a huge anime fan, my favorite is Hunter x Hunter, and I mostly code in Java"
            Output: {"preferences": ["Favorite anime is Hunter x Hunter", "Codes mostly in Java"]}
            """;

    @Override
    public List<String> extract(String userText) {
        try {
            String rawJson = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userText)
                    .call()
                    .content();

            String cleaned = rawJson.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
            }
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode prefs = root.get("preferences");
            if (prefs == null || !prefs.isArray()) return List.of();

            List<String> result = new ArrayList<>();
            for (JsonNode pref : prefs) {
                result.add(pref.asText());
            }
            return result;
        } catch (Exception e) {
            log.warn("Preference extraction failed: {}", e.getMessage());
            return List.of();
        }
    }
}
