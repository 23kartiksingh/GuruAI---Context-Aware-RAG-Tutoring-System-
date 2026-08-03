package com.guruai.document.service;

import com.guruai.document.dto.response.ChunkResponse;

import java.util.List;

/**
 * Contract for Hybrid Search over document chunks.
 *
 * <p>Combines Dense vector search (pgvector cosine similarity) with
 * BM25 keyword search (PostgreSQL FTS) using Reciprocal Rank Fusion.
 *
 * <p>Implemented by {@link com.guruai.document.service.impl.HybridSearchServiceImpl}.
 */
public interface HybridSearchService {

    /**
     * Run a hybrid search for the given query within a session.
     *
     * <p>Pipeline:
     * <ol>
     *   <li>Check Redis cache — return cached result if hit</li>
     *   <li>Dense search: embed query → pgvector ANN cosine similarity</li>
     *   <li>BM25 search: PostgreSQL {@code to_tsquery} on chunk content</li>
     *   <li>RRF merge: combine and re-rank both result lists</li>
     *   <li>Cache result in Redis with TTL</li>
     * </ol>
     *
     * @param query     natural-language query string
     * @param sessionId restrict search to chunks from this session
     * @param topK      number of final results after RRF merging
     * @return ranked list of relevant chunks, best first
     */
    List<ChunkResponse> search(String query, String sessionId, int topK);

    /**
     * Dense-only search (used internally and by tests).
     *
     * @param query     natural-language query
     * @param sessionId session filter
     * @param topK      number of results
     * @return chunks sorted by cosine similarity
     */
    List<ChunkResponse> denseSearch(String query, String sessionId, int topK);

    /**
     * BM25 keyword search only (used internally and by tests).
     *
     * @param query     keyword query (may contain boolean operators)
     * @param sessionId session filter
     * @param topK      number of results
     * @return chunks sorted by BM25 rank
     */
    List<ChunkResponse> keywordSearch(String query, String sessionId, int topK);
}
