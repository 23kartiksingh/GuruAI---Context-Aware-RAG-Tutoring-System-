package com.guruai.memory.service.impl;

import com.guruai.memory.dto.response.MemoryChatResponse;
import com.guruai.memory.dto.response.MemoryItemResponse;
import com.guruai.memory.entity.MemoryChatHistory;
import com.guruai.memory.repository.MemoryChatHistoryRepository;
import com.guruai.memory.service.MemoryChatService;
import com.guruai.memory.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryChatServiceImpl implements MemoryChatService {

    private final ChatClient chatClient;
    private final MemoryService memoryService;
    private final MemoryChatHistoryRepository chatHistoryRepository;

    private static final String SYSTEM_TEMPLATE = """
            You are GuruAI's Memory Assistant — a warm, friendly bot that helps students
            reflect on their learning preferences and study goals.
            
            Your job:
            1. Engage conversationally and encourage the student to share how they learn best.
            2. Ask follow-up questions about their study style, goals, and difficulties.
            3. Keep responses concise (2-3 sentences max).
            
            Current known preferences for this student:
            %s
            """;

    @Override
    @Transactional
    public MemoryChatResponse chat(UUID userId, String message) {
        // Build context from existing memory
        String memoryContext = memoryService.buildContextForAgent(userId);
        String systemPrompt = String.format(SYSTEM_TEMPLATE,
                memoryContext.isEmpty() ? "None yet — get to know this student!" : memoryContext);

        // Build recent conversation history for context
        List<MemoryChatHistory> recentHistory =
                chatHistoryRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                        .reversed();

        StringBuilder conversation = new StringBuilder();
        for (MemoryChatHistory turn : recentHistory) {
            conversation.append(turn.getRole()).append(": ").append(turn.getContent()).append("\n");
        }
        conversation.append("user: ").append(message);

        String reply = chatClient.prompt()
                .system(systemPrompt)
                .user(conversation.toString())
                .call()
                .content();

        // Persist both turns
        chatHistoryRepository.save(new MemoryChatHistory(userId, "user", message));
        chatHistoryRepository.save(new MemoryChatHistory(userId, "assistant", reply));

        // Silently extract preferences from user message
        memoryService.addMemoryFromText(userId, message);

        List<String> currentItems = memoryService.getMemoryItems(userId).stream()
                .map(MemoryItemResponse::text)
                .toList();
        return new MemoryChatResponse(reply, currentItems);
    }
}
