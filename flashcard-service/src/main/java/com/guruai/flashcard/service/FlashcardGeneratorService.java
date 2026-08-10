package com.guruai.flashcard.service;

import com.guruai.flashcard.entity.Flashcard;

import java.util.List;
import java.util.UUID;

public interface FlashcardGeneratorService {

    List<Flashcard> generateFromChunk(UUID userId, UUID sessionId,
                                       String subject, String topic,
                                       String chunkText, int count);
}
