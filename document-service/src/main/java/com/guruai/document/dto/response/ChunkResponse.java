package com.guruai.document.dto.response;

/**
 * A single chunk result from hybrid search.
 *
 * <p>Returned by {@code POST /documents/{sessionId}/search}.
 * Consumed by Study Agent Service for CRAG grading and answer generation.
 *
 * @param chunkId      UUID from the vector_store table
 * @param content      raw chunk text (passed to AI for answer generation)
 * @param documentId   which document this chunk came from
 * @param filename     human-readable source label (e.g. "chapter3.pdf")
 * @param chunkIndex   position within the document (for citation context)
 * @param similarityScore cosine similarity (0–1) from dense search
 * @param rrfScore     final Reciprocal Rank Fusion score (higher = more relevant)
 * @param retrievalMode which retrieval method surfaced this chunk: "dense" | "keyword" | "hybrid"
 */
public record ChunkResponse(
        String chunkId,
        String content,
        String documentId,
        String filename,
        int    chunkIndex,
        double similarityScore,
        double rrfScore,
        String retrievalMode
) {}
