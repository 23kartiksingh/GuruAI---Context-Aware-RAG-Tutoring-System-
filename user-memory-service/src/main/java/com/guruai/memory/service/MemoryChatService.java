package com.guruai.memory.service;

import com.guruai.memory.dto.response.MemoryChatResponse;

import java.util.UUID;

public interface MemoryChatService {
    /**
     * Engages the Memory Chat Bot — responds conversationally
     * while silently extracting new preferences from the message.
     */
    MemoryChatResponse chat(UUID userId, String message);
}
