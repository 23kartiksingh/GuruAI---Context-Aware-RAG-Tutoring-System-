package com.guruai.quiz.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guruai.quiz.client.DocumentServiceClient;
import com.guruai.quiz.dto.request.GenerateQuizRequest;
import com.guruai.quiz.entity.QuestionRef;
import com.guruai.quiz.service.QuizGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuizGeneratorServiceImpl implements QuizGeneratorService {

    // Same TOP_K as study-agent-service's CRAG retrieval — enough spread for
    // several distinct questions without a separate grading call. There's no
    // grading step here (unlike chat): a topic-scoped search over a single
    // session's documents is a small enough candidate pool that filtering
    // noise isn't worth a second Groq call, and every quiz-generation call
    // shares the same free-tier TPM budget as chat, so keeping this call
    // cheap matters.
    private static final int CONTEXT_TOP_K = 5;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final DocumentServiceClient documentClient;

    private static final String SYSTEM_PROMPT = """
            You are a quiz generation engine for an adaptive learning platform.
            Generate ONLY valid JSON. No markdown, no explanation, no preamble.
            The JSON must strictly follow this structure:
            {
              "questions": [
                {
                  "questionText": "...",
                  "options": ["A) ...", "B) ...", "C) ...", "D) ..."],
                  "correctAnswer": "A",
                  "explanation": "...",
                  "topic": "..."
                }
              ]
            }
            """;

    @Override
    public List<QuestionRef> generate(GenerateQuizRequest request) {
        String topic = request.topic() != null ? request.topic() : "general " + request.subject();

        // Ground the quiz in the student's actual uploaded documents when a
        // specific topic was given — the same reason study-agent-service's
        // CRAG pipeline exists for chat: a bare topic name is ambiguous on
        // its own (e.g. "Competition" could mean sports strategy or species
        // ecology) and the model will happily fill the gap with whatever its
        // general knowledge associates with that word. Searching the
        // session's documents removes that ambiguity.
        List<String> context = request.topic() != null
                ? documentClient.searchChunks(request.sessionId().toString(),
                        request.userId().toString(), request.topic(), CONTEXT_TOP_K)
                : List.of();

        String userPrompt = context.isEmpty()
                ? String.format(
                        "Generate exactly %d multiple-choice questions about '%s' in the subject '%s' " +
                        "at difficulty level %s.",
                        request.questionCount(), topic, request.subject(), request.difficulty().name())
                : String.format(
                        "Generate exactly %d multiple-choice questions at difficulty level %s, testing " +
                        "understanding of '%s' (subject: '%s'). Base the questions ONLY on the source " +
                        "material below, taken from the student's own uploaded documents — do not test " +
                        "facts that aren't supported by it. If the material doesn't cover something " +
                        "commonly associated with '%s', don't ask about it.\n\n" +
                        "SOURCE MATERIAL:\n%s",
                        request.questionCount(), request.difficulty().name(), topic, request.subject(),
                        topic, formatContext(context));

        log.info("Generating {} questions for subject={} topic={} groundedInDocs={}",
                 request.questionCount(), request.subject(), topic, !context.isEmpty());

        String rawJson = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();

        return parseQuestions(rawJson, topic);
    }

    private String formatContext(List<String> chunks) {
        return chunks.stream().map(c -> "---\n" + c).collect(Collectors.joining("\n"));
    }

    private List<QuestionRef> parseQuestions(String rawJson, String defaultTopic) {
        List<QuestionRef> refs = new ArrayList<>();
        try {
            // Strip markdown code fences if model adds them
            String cleaned = rawJson.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
            }
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode questions = root.get("questions");
            if (questions == null || !questions.isArray()) {
                log.warn("No 'questions' array found in AI response");
                return refs;
            }
            for (JsonNode q : questions) {
                QuestionRef ref = new QuestionRef();
                ref.setQuestionText(q.path("questionText").asText());
                ref.setCorrectAnswer(extractAnswerLetter(q.path("correctAnswer").asText("A")));
                ref.setExplanation(q.path("explanation").asText(""));
                ref.setTopic(q.path("topic").asText(defaultTopic));
                // Serialize options array back to JSON string
                JsonNode optionsNode = q.get("options");
                ref.setOptionsJson(optionsNode != null ?
                        objectMapper.writeValueAsString(optionsNode) : "[]");
                refs.add(ref);
            }
        } catch (Exception e) {
            log.error("Failed to parse AI quiz response: {}", e.getMessage());
        }
        return refs;
    }

    /**
     * Reduce whatever the model returned for "correctAnswer" down to a single
     * A/B/C/D letter — the {@code correct_answer} column is VARCHAR(1).
     *
     * <p>The system prompt asks for just the letter, but a small/fast model
     * like Groq's llama-3.1-8b-instant doesn't always comply exactly (e.g.
     * it can return "A) Test match" or "Option A" instead of "A") — that
     * used to insert the full string straight into a 1-char column and crash
     * the whole quiz-generation transaction with a Postgres "value too long"
     * error. Take the first A-D letter found instead of trusting the raw
     * string, and fall back to "A" (logging it) if none is present at all.
     */
    private String extractAnswerLetter(String rawAnswer) {
        String upper = rawAnswer == null ? "" : rawAnswer.strip().toUpperCase();
        for (char c : upper.toCharArray()) {
            if (c >= 'A' && c <= 'D') {
                return String.valueOf(c);
            }
        }
        log.warn("AI returned an unrecognized correctAnswer format: '{}' — defaulting to 'A'", rawAnswer);
        return "A";
    }
}
