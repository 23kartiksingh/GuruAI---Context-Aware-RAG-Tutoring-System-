package com.guruai.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request body for {@code POST /documents/{sessionId}/search}.
 *
 * @param query  the natural-language search query
 * @param topK   number of results to return (default 5)
 * @param userId the requesting user (validated against session ownership)
 */
public record SearchRequest(

        @NotBlank(message = "Search query is required")
        String query,

        @Positive
        int topK,

        @NotBlank
        String userId
) {
    /** Apply defaults if not specified. */
    public SearchRequest {
        if (topK <= 0) topK = 5;
    }
}
