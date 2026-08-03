package com.guruai.document.util;

import com.guruai.document.dto.response.ChunkResponse;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Implements Reciprocal Rank Fusion (RRF) to merge ranked result lists.
 *
 * <p>RRF formula (Cormack et al., 2009):
 * <pre>
 *   RRF_score(d, Q) = Σ over each query q in Q: 1 / (k + rank(d, q))
 * </pre>
 *
 * <p>Where:
 * <ul>
 *   <li>{@code k} = 60 (standard constant — reduces impact of top rankings)</li>
 *   <li>{@code rank(d, q)} = 1-based rank of document {@code d} in results for query {@code q}</li>
 * </ul>
 *
 * <p>Ported from Python {@code retriever.py} — this is a direct translation of the
 * {@code reciprocal_rank_fusion()} function in the original codebase.
 */
@Component
public class RrfMerger {

    /** Standard RRF constant. Value 60 is from the original Cormack et al. paper. */
    private static final int DEFAULT_K = 60;

    /**
     * Merge dense and BM25 keyword results using RRF.
     *
     * @param denseResults   ordered list of chunks from vector similarity search (best first)
     * @param keywordResults ordered list of chunks from BM25 keyword search (best first)
     * @param topK           how many results to return after merging
     * @return merged and re-ranked list of chunks, highest RRF score first
     */
    public List<ChunkResponse> merge(List<ChunkResponse> denseResults,
                                     List<ChunkResponse> keywordResults,
                                     int topK) {
        return merge(denseResults, keywordResults, topK, DEFAULT_K);
    }

    /**
     * Merge with a custom {@code k} constant.
     *
     * @param denseResults   vector search results (ranked, best first)
     * @param keywordResults BM25 results (ranked, best first)
     * @param topK           number of final results to return
     * @param k              RRF smoothing constant (typically 60)
     * @return merged, ranked, deduplicated chunk list
     */
    public List<ChunkResponse> merge(List<ChunkResponse> denseResults,
                                     List<ChunkResponse> keywordResults,
                                     int topK, int k) {
        // Map: chunkId → accumulated RRF score
        Map<String, Double>        rrfScores = new LinkedHashMap<>();

        // Map: chunkId → original ChunkResponse (to retrieve metadata after ranking)
        Map<String, ChunkResponse> chunkMap  = new LinkedHashMap<>();

        // Score dense results
        for (int i = 0; i < denseResults.size(); i++) {
            ChunkResponse chunk = denseResults.get(i);
            double score = 1.0 / (k + i + 1);
            rrfScores.merge(chunk.chunkId(), score, Double::sum);
            chunkMap.putIfAbsent(chunk.chunkId(), chunk);
        }

        // Score keyword results
        for (int i = 0; i < keywordResults.size(); i++) {
            ChunkResponse chunk = keywordResults.get(i);
            double score = 1.0 / (k + i + 1);
            rrfScores.merge(chunk.chunkId(), score, Double::sum);
            chunkMap.putIfAbsent(chunk.chunkId(), chunk);
        }

        // Sort by RRF score (descending), take topK
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    ChunkResponse original = chunkMap.get(entry.getKey());
                    double rrfScore = entry.getValue();

                    // Tag the retrieval mode
                    boolean inDense   = denseResults.stream()
                            .anyMatch(c -> c.chunkId().equals(entry.getKey()));
                    boolean inKeyword = keywordResults.stream()
                            .anyMatch(c -> c.chunkId().equals(entry.getKey()));
                    String mode = (inDense && inKeyword) ? "hybrid"
                                : inDense                ? "dense"
                                :                          "keyword";

                    return new ChunkResponse(
                            original.chunkId(),
                            original.content(),
                            original.documentId(),
                            original.filename(),
                            original.chunkIndex(),
                            original.similarityScore(),
                            rrfScore,
                            mode
                    );
                })
                .toList();
    }
}
