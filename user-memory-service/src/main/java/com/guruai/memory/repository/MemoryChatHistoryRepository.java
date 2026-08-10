package com.guruai.memory.repository;

import com.guruai.memory.entity.MemoryChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemoryChatHistoryRepository extends JpaRepository<MemoryChatHistory, UUID> {

    List<MemoryChatHistory> findByUserIdOrderByCreatedAtAsc(UUID userId);

    /** Returns the last N messages for context window (ordered oldest-first). */
    List<MemoryChatHistory> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);
}
