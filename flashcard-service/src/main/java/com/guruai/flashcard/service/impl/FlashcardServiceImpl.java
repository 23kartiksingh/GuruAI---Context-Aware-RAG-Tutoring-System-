package com.guruai.flashcard.service.impl;

import com.guruai.common.events.FlashcardReviewedEvent;
import com.guruai.common.exception.ResourceNotFoundException;
import com.guruai.flashcard.dto.response.FlashcardResponse;
import com.guruai.flashcard.dto.response.ReviewResultResponse;
import com.guruai.flashcard.entity.Flashcard;
import com.guruai.flashcard.event.producer.FlashcardEventProducer;
import com.guruai.flashcard.mapper.FlashcardMapper;
import com.guruai.flashcard.repository.FlashcardRepository;
import com.guruai.flashcard.service.FlashcardGeneratorService;
import com.guruai.flashcard.service.FlashcardService;
import com.guruai.flashcard.service.Sm2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlashcardServiceImpl implements FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final Sm2Service sm2Service;
    private final FlashcardGeneratorService generatorService;
    private final FlashcardEventProducer eventProducer;
    private final FlashcardMapper flashcardMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FlashcardResponse> getDueToday(UUID userId) {
        return flashcardRepository
                .findByUserIdAndNextReviewDateLessThanEqual(userId, LocalDate.now())
                .stream().map(flashcardMapper::toResponse).toList();
    }

    @Override
    public long getDueTodayCount(UUID userId) {
        return flashcardRepository.countByUserIdAndNextReviewDateLessThanEqual(
                userId, LocalDate.now());
    }

    @Override
    @Transactional
    public ReviewResultResponse review(UUID cardId, int quality) {
        Flashcard card = flashcardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found: " + cardId));

        int clamped = Math.max(0, Math.min(5, quality));
        sm2Service.applyReview(card, clamped);
        flashcardRepository.save(card);

        // Publish flashcard.reviewed for Knowledge Service
        eventProducer.publishFlashcardReviewed(FlashcardReviewedEvent.of(
                card.getId().toString(),
                card.getUserId().toString(),
                card.getSessionId() != null ? card.getSessionId().toString() : null,
                card.getTopic() != null ? card.getTopic() : "",
                card.getSubject() != null ? card.getSubject() : "",
                clamped
        ));

        return new ReviewResultResponse(
                card.getEaseFactor(), card.getIntervalDays(), card.getNextReviewDate());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashcardResponse> getByUser(UUID userId) {
        return flashcardRepository.findByUserId(userId)
                .stream().map(flashcardMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public void generateForSession(UUID userId, UUID sessionId, String subject,
                                   String topic, String chunkText) {
        List<Flashcard> generated = generatorService.generateFromChunk(
                userId, sessionId, subject, topic, chunkText, 5);
        if (!generated.isEmpty()) {
            flashcardRepository.saveAll(generated);
            log.info("Generated {} flashcards for userId={} sessionId={}",
                    generated.size(), userId, sessionId);
        }
    }

    @Override
    @Transactional
    public int deleteBySession(UUID userId, UUID sessionId) {
        List<Flashcard> cards = flashcardRepository.findByUserIdAndSessionId(userId, sessionId);
        flashcardRepository.deleteAll(cards);
        log.info("Deleted {} flashcard(s) for userId={} sessionId={}", cards.size(), userId, sessionId);
        return cards.size();
    }
}
