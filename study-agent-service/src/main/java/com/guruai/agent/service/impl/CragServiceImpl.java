package com.guruai.agent.service.impl;

import com.guruai.agent.client.DocumentServiceClient;
import com.guruai.agent.service.CragService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * See {@link CragService} for what CRAG is and why it exists.
 *
 * <p>Grading strategy (same as the Python prototype): all retrieved chunks
 * are graded in ONE batched LLM call that returns just the relevant chunk
 * IDs (e.g. {@code "0, 2"}) or the word {@code "none"} — one cheap call
 * per question instead of one per chunk, and parsing integers out of the
 * reply is far more robust than asking an 8B model for structured JSON.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CragServiceImpl implements CragService {

    private static final int TOP_K = 5;
    // Below this many retrieved chunks, grading isn't worth a whole extra
    // Groq round-trip — with 1-3 chunks there's little to filter down from,
    // so we skip straight to using them all. Saves one full LLM call on a
    // large share of turns (most sessions only have a handful of relevant
    // chunks for any given question anyway).
    private static final int SKIP_GRADING_BELOW = 3;
    // The grader only needs enough of each chunk to judge relevance, not the
    // whole thing — full text is still what goes into the final answer
    // context for whichever chunks survive grading. Roughly halves the
    // grading call's token cost on long chunks.
    private static final int GRADING_PREVIEW_CHARS = 400;
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private final DocumentServiceClient documentClient;
    private final ChatClient chatClient;

    private static final String GRADER_PROMPT = """
            You are a relevance grader for a Retrieval-Augmented Generation (RAG) pipeline.

            Your task: decide which of the retrieved DOCUMENTS contain information that is
            useful for answering the USER QUESTION.

            Respond with ONLY a comma-separated list of the integer IDs of the relevant
            documents (e.g., 0, 2). If NONE of the documents are relevant, respond with
            the exact word "none". Do not explain. Do not add any other text.

            USER QUESTION:
            %s

            RETRIEVED DOCUMENTS:
            %s

            Relevant document IDs:""";

    @Override
    public CragResult retrieveAndGrade(String sessionId, String userId, String question) {
        // ── Step 1: Vector retrieval via document-service ─────────────────
        List<String> rawChunks = documentClient.searchChunks(sessionId, userId, question, TOP_K);
        log.debug("CRAG: retrieved {} chunks for sessionId={}", rawChunks.size(), sessionId);

        if (rawChunks.isEmpty()) {
            return fallbackResult("No documents are uploaded in this session.");
        }

        // ── Step 2: LLM relevance grading ─────────────────────────────────
        // Skip the grading call entirely for small chunk sets — see
        // SKIP_GRADING_BELOW javadoc. Retrieval already did the real work of
        // narrowing the corpus down to TOP_K; grading exists to filter noise
        // out of a larger set, not to second-guess two or three results.
        List<String> relevant = rawChunks.size() <= SKIP_GRADING_BELOW
                ? rawChunks
                : gradeChunks(question, rawChunks);
        log.debug("CRAG: kept {}/{} chunks after grading", relevant.size(), rawChunks.size());

        if (relevant.isEmpty()) {
            return fallbackResult(
                    "Documents exist in this session, but none of the retrieved excerpts " +
                    "were relevant to this question after Corrective RAG filtering.");
        }

        // ── Step 3: Build prompt-ready context from survivors ─────────────
        String context = "Relevant excerpts from the student's uploaded documents:\n\n" +
                relevant.stream()
                        .map(c -> "---\n" + c)
                        .collect(Collectors.joining("\n"));
        return new CragResult(context, true, "[Your Documents]");
    }

    @Override
    public List<String> gradeChunks(String question, List<String> chunks) {
        if (chunks.isEmpty()) {
            return List.of();
        }

        // Truncated previews for grading only — the grader is deciding
        // relevance, not writing the answer, so it doesn't need full chunk
        // text. Whichever IDs it returns still map back to the FULL chunks
        // in the caller's `chunks` list for the actual answer context.
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String preview = chunk.length() > GRADING_PREVIEW_CHARS
                    ? chunk.substring(0, GRADING_PREVIEW_CHARS) + "..."
                    : chunk;
            formatted.append("--- Document ID: ").append(i).append(" ---\n")
                     .append(preview).append("\n\n");
        }

        String verdict;
        try {
            verdict = chatClient.prompt()
                    .user(String.format(GRADER_PROMPT, question, formatted))
                    .call()
                    .content();
        } catch (Exception e) {
            // Grader failure -> keep everything. A few irrelevant chunks in the
            // prompt beats silently throwing away context the answer needed.
            log.warn("CRAG grader call failed ({}), keeping all {} chunks", e.getMessage(), chunks.size());
            return chunks;
        }

        if (verdict == null || verdict.strip().equalsIgnoreCase("none")) {
            return List.of();
        }

        Set<Integer> keptIds = new HashSet<>();
        Matcher m = DIGITS.matcher(verdict);
        while (m.find()) {
            keptIds.add(Integer.parseInt(m.group()));
        }

        List<String> relevant = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            if (keptIds.contains(i)) {
                relevant.add(chunks.get(i));
            }
        }
        return relevant;
    }

    /**
     * Context used when there's nothing relevant to ground the answer in.
     *
     * <p>Deliberately states the situation as a plain fact and does NOT tell
     * the model to announce it. The disclaimer is asked for exactly once, in
     * the system prompt's rules (see StudyAgentServiceImpl) — this string
     * used to append "explicitly tell the student that this answer does NOT
     * come from their uploaded documents", which combined with that rule and
     * the trailing "Source:" line meant the model was instructed to disclaim
     * three times over, and duly wrote the same apology twice in a row.
     */
    private CragResult fallbackResult(String reason) {
        return new CragResult("No grounding context is available. Reason: " + reason,
                false, "[General Knowledge]");
    }
}
