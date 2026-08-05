package com.guruai.agent.service;

import java.util.List;

/**
 * Corrective RAG (CRAG) — the "check your sources before citing them" step.
 *
 * <p>Plain RAG stuffs whatever vector search returns straight into the
 * prompt. That fails quietly when the user's question has nothing to do
 * with their uploaded documents: cosine similarity always returns
 * <i>something</i>, and the LLM will happily hallucinate a connection to
 * irrelevant context. CRAG adds a cheap LLM grading pass in between:
 * retrieve → grade each chunk's relevance → keep only what passes →
 * fall back to general knowledge (and say so) when nothing does.
 *
 * <p>Ported from the original Python prototype's {@code crag.py}.
 */
public interface CragService {

    /**
     * Result of the retrieval + grading pipeline.
     *
     * @param context     text block ready for prompt injection (either the
     *                    surviving chunks, or fallback instructions telling
     *                    the model to answer from general knowledge and be
     *                    upfront that it isn't citing the user's documents)
     * @param usedRag     true if at least one chunk survived grading
     * @param sourceLabel "[Your Documents]" or "[General Knowledge]" — surfaced
     *                    to the student so they know where an answer came from
     */
    record CragResult(String context, boolean usedRag, String sourceLabel) {}

    /**
     * Run the full pipeline for a user question.
     *
     * @param sessionId session whose document embeddings to search
     * @param userId    the user asking (forwarded to document-service search)
     * @param question  the user's question
     * @return graded, prompt-ready context
     */
    CragResult retrieveAndGrade(String sessionId, String userId, String question);

    /**
     * Grade a set of already-retrieved chunks (exposed separately so tests
     * can exercise grading without a live vector store).
     *
     * @return the subset of chunks judged relevant to the question
     */
    List<String> gradeChunks(String question, List<String> chunks);
}
