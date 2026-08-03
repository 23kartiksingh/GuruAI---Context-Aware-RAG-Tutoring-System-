package com.guruai.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapped to {@code user_documents} in {@code document_db}.
 *
 * <p>Named {@code UserDocument} to avoid naming conflict with
 * Spring AI's {@link org.springframework.ai.document.Document} class.
 *
 * <p>Schema managed by Flyway — V1__create_user_documents_table.sql.
 *
 * <p><b>Note</b>: chunk content and embeddings are stored in Spring AI's
 * {@code vector_store} table, NOT here. This entity only holds metadata.
 */
@Entity
@Table(name = "user_documents")
@Getter
@Setter
@NoArgsConstructor
public class UserDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "filename", nullable = false, length = 500)
    private String filename;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DocumentStatus status;

    /**
     * AI-extracted list of academic topics from the document content.
     * Stored as a JSONB array, e.g. {@code ["Binary Trees","DFS","BFS"]}.
     * Mapped with Hypersistence Utils JsonType.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topics", columnDefinition = "jsonb")
    private List<String> topics = new ArrayList<>();

    /**
     * Primary detected subject (e.g. "Data Structures").
     * Extracted alongside topics via Spring AI Structured Output.
     */
    @Column(name = "subject", length = 200)
    private String subject;

    /** Set when status = FAILED. Contains the error details. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (status == null) status = DocumentStatus.PROCESSING;
        if (topics == null) topics = new ArrayList<>();
    }

    /** Factory for creating a document in PROCESSING state. */
    public static UserDocument create(UUID sessionId, UUID userId,
                                       String filename, String fileType,
                                       long fileSizeBytes) {
        UserDocument d = new UserDocument();
        d.sessionId     = sessionId;
        d.userId        = userId;
        d.filename      = filename;
        d.fileType      = fileType;
        d.fileSizeBytes = fileSizeBytes;
        d.status        = DocumentStatus.PROCESSING;
        return d;
    }

    /** Mark as successfully indexed. */
    public void markIndexed(int chunkCount, List<String> topics, String subject) {
        this.status     = DocumentStatus.INDEXED;
        this.chunkCount = chunkCount;
        this.topics     = topics != null ? topics : new ArrayList<>();
        this.subject    = subject;
    }

    /** Mark as failed with an error message. */
    public void markFailed(String errorMessage) {
        this.status       = DocumentStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
