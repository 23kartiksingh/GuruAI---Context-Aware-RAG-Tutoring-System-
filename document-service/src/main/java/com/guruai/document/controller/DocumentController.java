package com.guruai.document.controller;

import com.guruai.document.dto.request.SearchRequest;
import com.guruai.document.dto.response.ChunkResponse;
import com.guruai.document.dto.response.ConceptMapResponse;
import com.guruai.document.dto.response.DocumentResponse;
import com.guruai.document.service.DocumentService;
import com.guruai.document.service.HybridSearchService;
import com.guruai.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for all Document Service endpoints.
 *
 * <p>Base path: {@code /documents}
 *
 * <table border="1">
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>POST</td>
 *       <td>/documents/{sessionId}/upload</td>
 *       <td>Upload and index a document (async)</td></tr>
 *   <tr><td>GET</td>
 *       <td>/documents/{sessionId}</td>
 *       <td>List all documents in a session</td></tr>
 *   <tr><td>GET</td>
 *       <td>/documents/{sessionId}/{documentId}</td>
 *       <td>Get single document metadata</td></tr>
 *   <tr><td>GET</td>
 *       <td>/documents/{sessionId}/{documentId}/topics</td>
 *       <td>Get AI-extracted concept map</td></tr>
 *   <tr><td>POST</td>
 *       <td>/documents/{sessionId}/search</td>
 *       <td>Hybrid search over session chunks</td></tr>
 *   <tr><td>DELETE</td>
 *       <td>/documents/{sessionId}/{documentId}</td>
 *       <td>Delete document and its chunks</td></tr>
 * </table>
 *
 * <p><b>Auth</b>: Gateway injects {@code X-User-Id} header after verifying JWT.
 * All endpoints read userId from this header (not from JWT directly).
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService    documentService;
    private final HybridSearchService hybridSearchService;

    /**
     * Upload a document file (PDF, DOCX, TXT, image) for indexing.
     *
     * <p>Returns immediately with status=PROCESSING.
     * Client should poll {@code GET /documents/{sessionId}/{documentId}} for status=INDEXED.
     *
     * @param sessionId  path variable — the study session
     * @param userId     injected by gateway from JWT
     * @param file       multipart file upload
     * @return 202 Accepted with initial document metadata
     */
    @PostMapping(value = "/{sessionId}/upload",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @PathVariable String sessionId,
            @RequestHeader("X-User-Id") String userId,
            @RequestPart("file") MultipartFile file) {

        DocumentResponse response = documentService.uploadAndIndex(file, sessionId, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(true, HttpStatus.ACCEPTED.value(), response, null,
                        "Document received — processing started", Instant.now()));
    }

    /**
     * List all documents in a study session.
     *
     * @param sessionId the session to list
     * @param userId    gateway-injected user ID for access control
     * @return 200 OK with list of document metadata
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> listBySession(
            @PathVariable String sessionId,
            @RequestHeader("X-User-Id") String userId) {

        List<DocumentResponse> docs = documentService.listBySession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }

    /**
     * Get metadata for a single document (including indexing status).
     *
     * @param sessionId  path variable
     * @param documentId path variable
     * @return 200 OK with document metadata
     */
    @GetMapping("/{sessionId}/{documentId}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(
            @PathVariable String sessionId,
            @PathVariable String documentId) {

        DocumentResponse doc = documentService.getById(documentId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(doc));
    }

    /**
     * Get the AI-extracted concept map (topics + subject) for a document.
     *
     * @param sessionId  path variable
     * @param documentId path variable
     * @return 200 OK with concept map
     */
    @GetMapping("/{sessionId}/{documentId}/topics")
    public ResponseEntity<ApiResponse<ConceptMapResponse>> getTopics(
            @PathVariable String sessionId,
            @PathVariable String documentId) {

        ConceptMapResponse topics = documentService.getConceptMap(documentId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(topics));
    }

    /**
     * Hybrid search over chunks in a session.
     *
     * <p>This is the core retrieval endpoint consumed by Study Agent Service.
     * Uses Dense + BM25 + RRF with Redis caching.
     *
     * @param sessionId path variable
     * @param request   search parameters
     * @return 200 OK with ranked chunk list
     */
    @PostMapping("/{sessionId}/search")
    public ResponseEntity<ApiResponse<List<ChunkResponse>>> search(
            @PathVariable String sessionId,
            @Valid @RequestBody SearchRequest request) {

        List<ChunkResponse> results = hybridSearchService.search(
                request.query(), sessionId, request.topK());
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    /**
     * Delete a document and all its pgvector chunks.
     *
     * @param sessionId  path variable
     * @param documentId path variable
     * @param userId     gateway-injected user ID for ownership check
     * @return 204 No Content
     */
    @DeleteMapping("/{sessionId}/{documentId}")
    public ResponseEntity<Void> delete(
            @PathVariable String sessionId,
            @PathVariable String documentId,
            @RequestHeader("X-User-Id") String userId) {

        documentService.delete(documentId, sessionId, userId);
        return ResponseEntity.noContent().build();
    }
}
