package com.guruai.agent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A single message in a study session (user or assistant turn).
 * Persisted here as the sole source of conversation history — each chat turn
 * re-queries the most recent messages for this session directly via
 * {@link com.guruai.agent.repository.MessageRepository} (see
 * StudyAgentServiceImpl), rather than a separate in-memory/Redis-backed
 * ChatMemory abstraction.
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    /** "user" or "assistant" */
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Message(Session session, String role, String content) {
        this.session = session;
        this.role = role;
        this.content = content;
    }
}
