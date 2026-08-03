package com.guruai.document.service.impl;

import com.guruai.document.config.DocumentProperties;
import com.guruai.document.dto.response.ChunkResponse;
import com.guruai.document.service.HybridSearchService;
import com.guruai.document.util.RrfMerger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link HybridSearchService}.
 *
 * <p>Hybrid pipeline (ported from Python {@code retriever.py}):
 * <ol>
 *   <li><b>Cache check</b> — Redis key {@code retriever:cache:{sessionId}:{queryHash}}</li>
 *   <li><b>Dense search</b> — Spring AI VectorStore cosine similarity with session filter</li>
 *   <li><b>BM25 keyword</b> — PostgreSQL {@code to_tsquery} on {@code vector_store.content}</li>
 *   <li><b>RRF merge</b> — Reciprocal Rank Fusion (k=60) → topK final results</li>
 *   <li><b>Cache store</b> — write result to Redis with configured TTL</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchServiceImpl implements HybridSearchService {

    private static final String CACHE_PREFIX = "retriever:cache:";

    private final VectorStore                  vectorStore;
    private final JdbcTemplate                 jdbcTemplate;
    private final RrfMerger                    rrfMerger;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DocumentProperties           props;

    // ── Public API ─────────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<ChunkResponse> search(String query, String sessionId, int topK) {
        String cacheKey = buildCacheKey(sessionId, query);

        // 1. Try Redis cache
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List<?> list && !list.isEmpty()) {
            log.debug("Retriever cache HIT for sessionId={} query='{}'", sessionId, abbreviate(query));
            return (List<ChunkResponse>) list;
        }

        int topKDense   = props.retriever().topKDense();
        int topKKeyword = props.retriever().topKKeyword();
        int rrfK        = props.retriever().rrfK();

        // 2. Dense search
        List<ChunkResponse> denseResults = denseSearch(query, sessionId, topKDense);
        log.debug("Dense search returned {} results", denseResults.size());

        // 3. BM25 keyword search
        List<ChunkResponse> keywordResults = keywordSearch(query, sessionId, topKKeyword);
        log.debug("Keyword search returned {} results", keywordResults.size());

        // 4. RRF merge
        List<ChunkResponse> merged = rrfMerger.merge(denseResults, keywordResults, topK, rrfK);
        log.info("Hybrid search: dense={} keyword={} merged={} (sessionId={})",
                 denseResults.size(), keywordResults.size(), merged.size(), sessionId);

        // 5. Cache result
        Duration ttl = Duration.ofSeconds(props.retriever().cacheTtlSeconds());
        redisTemplate.opsForValue().set(cacheKey, new ArrayList<>(merged), ttl);

        return merged;
    }

    @Override
    public List<ChunkResponse> denseSearch(String query, String sessionId, int topK) {
        // Build metadata filter: only search chunks from this session
        var filterBuilder = new FilterExpressionBuilder();
        var filter = filterBuilder.eq("session_id", sessionId).build();

        var searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(filter)
                .similarityThreshold(0.0)   // return all candidates (RRF decides)
                .build();

        return vectorStore.similaritySearch(searchRequest).stream()
                .map(doc -> {
                    Map<String, Object> meta = doc.getMetadata();
                    // Similarity score comes from Document.getScore() (set by the
                    // vector store during search), not a "distance" metadata key —
                    // that key was never actually populated, so this used to
                    // silently score every dense result as 0.0 and skew RRF
                    // ranking toward keyword-only results.
                    double score = doc.getScore() != null ? doc.getScore() : 0.0;
                    return new ChunkResponse(
                            doc.getId(),
                            doc.getFormattedContent(),
                            getString(meta, "document_id"),
                            getString(meta, "filename"),
                            getInt(meta, "chunk_index"),
                            score,
                            0.0,   // rrfScore set by merger
                            "dense"
                    );
                })
                .toList();
    }

    @Override
    public List<ChunkResponse> keywordSearch(String query, String sessionId, int topK) {
        /*
         * PostgreSQL full-text BM25-style ranking.
         *
         * We cast metadata (JSON) to JSONB inline for the session filter.
         * ts_rank_cd uses cover density ranking (BM25-adjacent).
         * plainto_tsquery handles natural language input safely (no syntax injection).
         */
        String sql = """
                SELECT
                    id::text                                       AS chunk_id,
                    content,
                    metadata->>'document_id'                       AS document_id,
                    metadata->>'filename'                          AS filename,
                    COALESCE((metadata->>'chunk_index')::int, 0)   AS chunk_index,
                    ts_rank_cd(
                        to_tsvector('english', COALESCE(content, '')),
                        plainto_tsquery('english', ?),
                        32
                    ) AS rank
                FROM vector_store
                WHERE
                    metadata->>'session_id' = ?
                    AND to_tsvector('english', COALESCE(content, ''))
                        @@ plainto_tsquery('english', ?)
                ORDER BY rank DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new ChunkResponse(
                        rs.getString("chunk_id"),
                        rs.getString("content"),
                        rs.getString("document_id"),
                        rs.getString("filename"),
                        rs.getInt("chunk_index"),
                        rs.getDouble("rank"),
                        0.0,      // rrfScore set by merger
                        "keyword"
                ),
                query, sessionId, query, topK
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildCacheKey(String sessionId, String query) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(query.getBytes(StandardCharsets.UTF_8));
            return CACHE_PREFIX + sessionId + ":" + HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return CACHE_PREFIX + sessionId + ":" + query.hashCode();
        }
    }

    private String getString(Map<String, Object> meta, String key) {
        Object v = meta.get(key);
        return v != null ? v.toString() : "";
    }

    private int getInt(Map<String, Object> meta, String key) {
        Object v = meta.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s)  return Integer.parseInt(s);
        return 0;
    }

    private double getDouble(Map<String, Object> meta, String key) {
        Object v = meta.get(key);
        if (v instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private String abbreviate(String s) {
        return s.length() > 50 ? s.substring(0, 50) + "..." : s;
    }
}
