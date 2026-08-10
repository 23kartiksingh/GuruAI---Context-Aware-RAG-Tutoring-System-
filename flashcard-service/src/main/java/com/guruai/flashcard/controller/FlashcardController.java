package com.guruai.flashcard.controller;

import com.guruai.common.dto.ApiResponse;
import com.guruai.flashcard.dto.request.ReviewRequest;
import com.guruai.flashcard.dto.response.FlashcardResponse;
import com.guruai.flashcard.dto.response.ReviewResultResponse;
import com.guruai.flashcard.service.FlashcardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;

    /** Cards due for review today (SM-2 schedule). */
    @GetMapping("/{userId}/due-today")
    public ResponseEntity<ApiResponse<List<FlashcardResponse>>> getDueToday(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(flashcardService.getDueToday(userId)));
    }

    /** Count of cards due today (used by learning-path in study-agent). */
    @GetMapping("/{userId}/due-count")
    public ResponseEntity<ApiResponse<Long>> getDueTodayCount(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(flashcardService.getDueTodayCount(userId)));
    }

    /** Submit a SM-2 quality review for a single card. */
    @PostMapping("/{cardId}/review")
    public ResponseEntity<ApiResponse<ReviewResultResponse>> review(
            @PathVariable UUID cardId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(flashcardService.review(cardId, request.quality())));
    }

    /** All flashcards for a user. */
    @GetMapping("/{userId}/all")
    public ResponseEntity<ApiResponse<List<FlashcardResponse>>> getAll(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(flashcardService.getByUser(userId)));
    }
}
