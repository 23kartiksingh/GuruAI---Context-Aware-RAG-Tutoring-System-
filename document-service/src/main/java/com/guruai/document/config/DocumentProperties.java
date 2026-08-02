package com.guruai.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Strongly-typed binding for {@code guruai.document.*} and
 * {@code guruai.retriever.*} in application.yml.
 */
@ConfigurationProperties(prefix = "guruai")
public record DocumentProperties(
        DocumentConfig   document,
        RetrieverConfig  retriever
) {
    /**
     * @param chunkSize          characters per chunk (default 1000)
     * @param chunkOverlap       character overlap between adjacent chunks (default 150)
     * @param maxFileSizeMb      maximum accepted upload size in MB
     * @param allowedContentTypes MIME types accepted by the upload endpoint
     */
    public record DocumentConfig(
            int          chunkSize,
            int          chunkOverlap,
            int          maxFileSizeMb,
            List<String> allowedContentTypes
    ) {}

    /**
     * @param topKDense        candidates from vector cosine similarity search
     * @param topKKeyword      candidates from BM25 full-text search
     * @param topKFinal        results returned after RRF merging
     * @param rrfK             RRF smoothing constant (60 is standard)
     * @param cacheTtlSeconds  Redis cache TTL for query results
     */
    public record RetrieverConfig(
            int topKDense,
            int topKKeyword,
            int topKFinal,
            int rrfK,
            int cacheTtlSeconds
    ) {}
}
