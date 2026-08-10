package com.guruai.memory.controller;

import com.guruai.common.dto.ApiResponse;
import com.guruai.memory.dto.request.AddMemoryRequest;
import com.guruai.memory.dto.request.MemoryChatRequest;
import com.guruai.memory.dto.request.UpdateMemoryItemRequest;
import com.guruai.memory.dto.response.MemoryChatResponse;
import com.guruai.memory.dto.response.MemoryItemResponse;
import com.guruai.memory.dto.response.MemoryItemsResponse;
import com.guruai.memory.service.MemoryChatService;
import com.guruai.memory.service.MemoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
public class UserMemoryController {

    private final MemoryService memoryService;
    private final MemoryChatService memoryChatService;

    /** Get all stored memory items for a user. */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<MemoryItemsResponse>> getMemory(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(itemsResponse(userId)));
    }

    /** Add a memory item by extracting preferences from freeform text. */
    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<MemoryItemsResponse>> addMemory(
            @PathVariable UUID userId,
            @Valid @RequestBody AddMemoryRequest request) {
        memoryService.addMemoryFromText(userId, request.message());
        return ResponseEntity.ok(ApiResponse.ok(itemsResponse(userId)));
    }

    /**
     * Directly overwrite one item's text (a user edit, not extraction — no
     * LLM call, whatever they typed is stored as-is).
     */
    @PutMapping("/{userId}/items/{itemId}")
    public ResponseEntity<ApiResponse<MemoryItemsResponse>> updateMemoryItem(
            @PathVariable UUID userId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateMemoryItemRequest request) {
        memoryService.updateMemoryItem(userId, itemId, request.text());
        return ResponseEntity.ok(ApiResponse.ok(itemsResponse(userId)));
    }

    /** Remove one stored preference. */
    @DeleteMapping("/{userId}/items/{itemId}")
    public ResponseEntity<ApiResponse<MemoryItemsResponse>> deleteMemoryItem(
            @PathVariable UUID userId,
            @PathVariable UUID itemId) {
        memoryService.deleteMemoryItem(userId, itemId);
        return ResponseEntity.ok(ApiResponse.ok(itemsResponse(userId)));
    }

    /** Clear all stored memories for a user. */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<String>> clearMemory(@PathVariable UUID userId) {
        memoryService.clearMemory(userId);
        return ResponseEntity.ok(ApiResponse.ok("Memory cleared for userId: " + userId));
    }

    private MemoryItemsResponse itemsResponse(UUID userId) {
        List<MemoryItemResponse> items = memoryService.getMemoryItems(userId);
        return new MemoryItemsResponse(items, items.size());
    }

    /** Get raw memory context string (for Study Agent injection). */
    @GetMapping("/{userId}/context")
    public ResponseEntity<ApiResponse<String>> getContext(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                memoryService.buildContextForAgent(userId)));
    }

    /** Memory Chat Bot — conversational preference gathering. */
    @PostMapping("/{userId}/chat")
    public ResponseEntity<ApiResponse<MemoryChatResponse>> chat(
            @PathVariable UUID userId,
            @Valid @RequestBody MemoryChatRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                memoryChatService.chat(userId, request.message())));
    }
}
