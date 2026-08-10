package com.guruai.memory.service.impl;

import com.guruai.common.exception.ResourceNotFoundException;
import com.guruai.memory.dto.response.MemoryItemResponse;
import com.guruai.memory.entity.UserMemory;
import com.guruai.memory.repository.UserMemoryRepository;
import com.guruai.memory.service.MemoryService;
import com.guruai.memory.service.PreferenceExtractorService;
import com.guruai.memory.util.MemoryContextBuilder;
import com.guruai.memory.util.MemoryHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {

    private final UserMemoryRepository memoryRepository;
    private final PreferenceExtractorService extractorService;

    @Override
    @Transactional(readOnly = true)
    public List<MemoryItemResponse> getMemoryItems(UUID userId) {
        return memoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(m -> new MemoryItemResponse(m.getId(), m.getItem()))
                .toList();
    }

    @Override
    @Transactional
    public void addMemoryFromText(UUID userId, String message) {
        List<String> extracted = extractorService.extract(message);
        int saved = 0;
        for (String item : extracted) {
            String hash = MemoryHashUtil.sha256(item);
            if (!memoryRepository.existsByUserIdAndItemHash(userId, hash)) {
                memoryRepository.save(new UserMemory(userId, item, hash));
                saved++;
            }
        }
        if (saved > 0) {
            log.info("Saved {} new memory items for userId={}", saved, userId);
        }
    }

    @Override
    @Transactional
    public void updateMemoryItem(UUID userId, UUID itemId, String newText) {
        UserMemory item = memoryRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Memory item", itemId.toString()));
        item.setItem(newText);
        item.setItemHash(MemoryHashUtil.sha256(newText));
        memoryRepository.save(item);
        log.debug("Updated memory item {} for userId={}", itemId, userId);
    }

    @Override
    @Transactional
    public void deleteMemoryItem(UUID userId, UUID itemId) {
        UserMemory item = memoryRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Memory item", itemId.toString()));
        memoryRepository.delete(item);
        log.debug("Deleted memory item {} for userId={}", itemId, userId);
    }

    @Override
    @Transactional
    public void clearMemory(UUID userId) {
        memoryRepository.deleteByUserId(userId);
        log.info("Cleared all memory for userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public String buildContextForAgent(UUID userId) {
        List<String> items = memoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(UserMemory::getItem).toList();
        return MemoryContextBuilder.buildContext(items);
    }
}
