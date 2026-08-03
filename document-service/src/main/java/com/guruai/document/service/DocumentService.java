package com.guruai.document.service;

import com.guruai.document.dto.response.ConceptMapResponse;
import com.guruai.document.dto.response.DocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Contract for document upload, indexing, and management.
 *
 * <p>Implemented by {@link com.guruai.document.service.impl.DocumentServiceImpl}.
 */
public interface DocumentService {

    /**
     * Receive an uploaded file, parse it with Tika, chunk it, embed each chunk
     * with Gemini, store in pgvector, extract topics, and publish {@code document.indexed}.
     *
     * <p>This operation is intentionally async — the HTTP response returns immediately
     * with status PROCESSING; the client polls until status=INDEXED.
     *
     * @param file      the uploaded multipart file
     * @param sessionId the study session receiving this document
     * @param userId    the uploading user
     * @return initial {@link DocumentResponse} with status=PROCESSING
     */
    DocumentResponse uploadAndIndex(MultipartFile file, String sessionId, String userId);

    /**
     * List all documents in a session.
     *
     * @param sessionId the session to list
     * @param userId    for access control validation
     * @return list of document metadata, newest first
     */
    List<DocumentResponse> listBySession(String sessionId, String userId);

    /**
     * Get metadata for a single document.
     *
     * @param documentId UUID of the document
     * @param sessionId  for access control
     * @return document metadata
     * @throws com.guruai.common.exception.ResourceNotFoundException if not found
     */
    DocumentResponse getById(String documentId, String sessionId);

    /**
     * Get the AI-extracted concept map (topics + subject) for a document.
     *
     * @param documentId UUID of the document
     * @param sessionId  for access control
     * @return concept map response
     */
    ConceptMapResponse getConceptMap(String documentId, String sessionId);

    /**
     * Delete a document and all its chunks from pgvector.
     *
     * @param documentId UUID to delete
     * @param sessionId  for access control
     * @param userId     for access control
     */
    void delete(String documentId, String sessionId, String userId);

    /**
     * Delete every document in a session, plus their pgvector chunks.
     * Triggered by {@code session.deleted}.
     *
     * @return how many documents were removed
     */
    int deleteBySession(String sessionId, String userId);
}
