package com.guruai.memory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** One turn of the Memory Chat Bot conversation (for displaying history to the user). */
@Entity
@Table(name = "memory_chat_history")
@Getter
@Setter
@NoArgsConstructor
public class MemoryChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

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

    public MemoryChatHistory(UUID userId, String role, String content) {
        this.userId = userId;
        this.role = role;
        this.content = content;
    }
}
