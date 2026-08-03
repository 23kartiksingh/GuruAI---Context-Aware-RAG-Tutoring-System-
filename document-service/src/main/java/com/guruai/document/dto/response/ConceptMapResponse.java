package com.guruai.document.dto.response;

import java.util.List;

/**
 * Response for the AI-extracted concept map of a document.
 *
 * <p>Returned by {@code GET /documents/{sessionId}/{documentId}/topics}
 * and also included in the {@code document.indexed} Kafka event.
 *
 * @param documentId extracted topics belong to this document
 * @param filename   original file name
 * @param topics     list of specific academic sub-topics
 * @param subject    primary broad subject area (e.g. "Data Structures")
 */
public record ConceptMapResponse(
        String       documentId,
        String       filename,
        List<String> topics,
        String       subject
) {}
