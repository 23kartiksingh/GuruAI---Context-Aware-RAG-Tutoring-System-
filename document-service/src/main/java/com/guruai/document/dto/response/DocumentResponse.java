package com.guruai.document.dto.response;

import com.guruai.document.entity.DocumentStatus;

import java.time.Instant;
import java.util.List;

/**
 * Response for document metadata endpoints.
 *
 * @param documentId  UUID of the uploaded document
 * @param sessionId   which session it belongs to
 * @param filename    original uploaded filename
 * @param fileType    content type (pdf | docx | txt | image)
 * @param fileSizeMb  file size in MB (rounded)
 * @param chunkCount  number of chunks stored in pgvector (0 while PROCESSING)
 * @param status      current processing status
 * @param topics      AI-extracted academic topic list
 * @param subject     primary detected subject
 * @param createdAt   upload timestamp
 */
public record DocumentResponse(
        String         documentId,
        String         sessionId,
        String         filename,
        String         fileType,
        double         fileSizeMb,
        int            chunkCount,
        DocumentStatus status,
        List<String>   topics,
        String         subject,
        Instant        createdAt
) {}
