package com.guruai.memory.service;

import com.guruai.memory.dto.response.MemoryItemResponse;

import java.util.List;
import java.util.UUID;

public interface MemoryService {

    List<MemoryItemResponse> getMemoryItems(UUID userId);

    void addMemoryFromText(UUID userId, String message);

    /**
     * Directly overwrite one item's text — a user-initiated edit, not an
     * extraction. Whatever they type is stored verbatim.
     *
     * @throws com.guruai.common.exception.ResourceNotFoundException if the item
     *         doesn't exist or doesn't belong to this user
     */
    void updateMemoryItem(UUID userId, UUID itemId, String newText);

    /**
     * @throws com.guruai.common.exception.ResourceNotFoundException if the item
     *         doesn't exist or doesn't belong to this user
     */
    void deleteMemoryItem(UUID userId, UUID itemId);

    void clearMemory(UUID userId);

    String buildContextForAgent(UUID userId);
}
