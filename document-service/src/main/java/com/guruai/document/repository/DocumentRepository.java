package com.guruai.document.repository;

import com.guruai.document.entity.DocumentStatus;
import com.guruai.document.entity.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UserDocument} entities.
 */
@Repository
public interface DocumentRepository extends JpaRepository<UserDocument, UUID> {

    /** All documents in a session, newest first. */
    List<UserDocument> findBySessionIdOrderByCreatedAtDesc(UUID sessionId);

    /** All indexed documents for a user (all sessions). */
    List<UserDocument> findByUserIdAndStatus(UUID userId, DocumentStatus status);

    /** Single document — scoped to session for access control. */
    Optional<UserDocument> findByIdAndSessionId(UUID id, UUID sessionId);

    /** Count documents by status (for health checks). */
    long countByStatus(DocumentStatus status);

    /**
     * Delete all documents in a session (cascade removes vector_store entries separately).
     *
     * @param sessionId the session being deleted
     */
    @Modifying
    @Query("DELETE FROM UserDocument d WHERE d.sessionId = :sessionId")
    void deleteAllBySessionId(UUID sessionId);
}
