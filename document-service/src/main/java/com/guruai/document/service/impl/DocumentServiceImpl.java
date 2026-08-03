package com.guruai.document.service.impl;

import com.guruai.common.exception.GuruAIException;
import com.guruai.common.exception.ResourceNotFoundException;
import com.guruai.document.config.DocumentProperties;
import com.guruai.document.dto.response.ConceptMapResponse;
import com.guruai.document.dto.response.DocumentResponse;
import com.guruai.document.entity.UserDocument;
import com.guruai.document.event.producer.DocumentEventProducer;
import com.guruai.document.mapper.DocumentMapper;
import com.guruai.document.repository.DocumentRepository;
import com.guruai.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Implementation of {@link DocumentService}.
 *
 * <p>Pipeline for {@link #uploadAndIndex}:
 * <ol>
 *   <li>Validate content type and file size</li>
 *   <li>Save metadata row with status=PROCESSING</li>
 *   <li>Hand off to {@link DocumentIndexingWorker} (a separate bean so its
 *       {@code @Async} actually applies — see that class's javadoc) and
 *       return immediately; the client polls status until INDEXED</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository     documentRepository;
    private final DocumentIndexingWorker indexingWorker;
    private final JdbcTemplate           jdbcTemplate;
    private final DocumentEventProducer  eventProducer;
    private final DocumentMapper         documentMapper;
    private final DocumentProperties     props;

    // ── Upload & Index ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DocumentResponse uploadAndIndex(MultipartFile file, String sessionId, String userId) {
        // 1. Validate file
        validateFile(file);

        // 2. Persist metadata in PROCESSING state
        String mimeType  = Objects.requireNonNullElse(file.getContentType(), "application/octet-stream");
        UserDocument doc = UserDocument.create(
                UUID.fromString(sessionId),
                UUID.fromString(userId),
                Objects.requireNonNullElse(file.getOriginalFilename(), "unnamed"),
                mimeType,
                file.getSize()
        );
        doc = documentRepository.save(doc);
        log.info("Document saved as PROCESSING: id={} file='{}'", doc.getId(), doc.getFilename());

        // 3. Read file bytes now (before async — InputStream closes after response)
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            doc.markFailed("Failed to read file bytes: " + e.getMessage());
            documentRepository.save(doc);
            throw new GuruAIException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read upload");
        }

        // 4. Kick off async processing (doesn't block the HTTP response).
        // Cross-bean call so the worker's @Async proxy actually engages —
        // the old this.processDocumentAsync(...) version ran synchronously.
        //
        // Deferred to afterCommit: this method is still @Transactional and
        // hasn't committed yet at this line. If we called the worker
        // directly here, its @Async thread opens its OWN connection and,
        // under READ_COMMITTED isolation, can't see the row we just saved
        // until this transaction commits — its findById() would throw
        // NoSuchElementException almost every time (observed in testing:
        // the worker ran ~0.8s after the save but still couldn't find it).
        // registerSynchronization's afterCommit callback guarantees the
        // worker only starts once the INSERT is actually visible.
        UserDocument savedDoc = doc;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                indexingWorker.processDocument(savedDoc.getId(), fileBytes, savedDoc.getFilename(), sessionId);
            }
        });

        return documentMapper.toResponse(doc);
    }

    // ── Queries ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> listBySession(String sessionId, String userId) {
        return documentRepository
                .findBySessionIdOrderByCreatedAtDesc(UUID.fromString(sessionId))
                .stream()
                .map(documentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getById(String documentId, String sessionId) {
        UserDocument doc = documentRepository
                .findByIdAndSessionId(UUID.fromString(documentId), UUID.fromString(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
        return documentMapper.toResponse(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public ConceptMapResponse getConceptMap(String documentId, String sessionId) {
        UserDocument doc = documentRepository
                .findByIdAndSessionId(UUID.fromString(documentId), UUID.fromString(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
        return new ConceptMapResponse(
                doc.getId().toString(),
                doc.getFilename(),
                doc.getTopics(),
                doc.getSubject()
        );
    }

    @Override
    @Transactional
    public void delete(String documentId, String sessionId, String userId) {
        UserDocument doc = documentRepository
                .findByIdAndSessionId(UUID.fromString(documentId), UUID.fromString(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        // Delete chunks from vector_store by document_id metadata
        jdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata->>'document_id' = ?",
                documentId
        );

        documentRepository.delete(doc);
        log.info("Deleted document {} and {} chunks", documentId, doc.getChunkCount());
    }

    @Override
    @Transactional
    public int deleteBySession(String sessionId, String userId) {
        UUID session = UUID.fromString(sessionId);
        List<UserDocument> docs = documentRepository.findBySessionIdOrderByCreatedAtDesc(session);

        // Only touch documents actually owned by the user named in the event,
        // so a malformed event can't wipe someone else's library.
        UUID owner = UUID.fromString(userId);
        List<UserDocument> owned = docs.stream()
                .filter(d -> owner.equals(d.getUserId()))
                .toList();

        for (UserDocument doc : owned) {
            jdbcTemplate.update(
                    "DELETE FROM vector_store WHERE metadata->>'document_id' = ?",
                    doc.getId().toString());
            documentRepository.delete(doc);
        }

        log.info("Deleted {} document(s) and their chunks for sessionId={}", owned.size(), sessionId);
        return owned.size();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────
    // (Parsing/chunking/topic-extraction/embedding all moved to
    //  DocumentIndexingWorker — this class only handles the synchronous
    //  request/response side now.)

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GuruAIException(HttpStatus.BAD_REQUEST, "File is empty or missing");
        }

        long maxBytes = (long) props.document().maxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new GuruAIException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File size exceeds maximum of " + props.document().maxFileSizeMb() + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !props.document().allowedContentTypes().contains(contentType)) {
            throw new GuruAIException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "File type '" + contentType + "' is not supported. " +
                    "Accepted types: " + props.document().allowedContentTypes());
        }
    }

}
