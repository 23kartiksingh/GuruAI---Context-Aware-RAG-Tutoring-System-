package com.guruai.memory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores a single extracted preference item for a user.
 * Example items: "prefers visual explanations", "studying for GATE CS", "struggles with recursion"
 */
@Entity
@Table(
    name = "user_memories",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_memory_item",
        columnNames = {"user_id", "item_hash"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class UserMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** The preference/memory text (e.g. "prefers analogies over formulas"). */
    @Column(name = "item", nullable = false, columnDefinition = "TEXT")
    private String item;

    /**
     * SHA-256 hash of normalized item text — used for deduplication.
     * Prevents storing the same preference twice.
     */
    @Column(name = "item_hash", nullable = false, length = 64)
    private String itemHash;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UserMemory(UUID userId, String item, String itemHash) {
        this.userId = userId;
        this.item = item;
        this.itemHash = itemHash;
    }
}
