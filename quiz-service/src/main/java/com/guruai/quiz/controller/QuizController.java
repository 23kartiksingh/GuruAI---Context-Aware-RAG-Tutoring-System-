package com.guruai.quiz.controller;

import com.guruai.common.dto.ApiResponse;
import com.guruai.quiz.dto.request.GenerateQuizRequest;
import com.guruai.quiz.dto.request.SubmitAnswerRequest;
import com.guruai.quiz.dto.response.AnswerResultResponse;
import com.guruai.quiz.dto.response.QuizResponse;
import com.guruai.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
// "/quizzes" (plural) — must match api-gateway's route predicate
// (Path=/quizzes/**) and follows the same plural convention as
// /documents, /flashcards, /notifications. Was "/quiz" before, which
// made every gateway-routed quiz request 404 at this service.
@RequestMapping("/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /** Generate a new adaptive quiz. */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<QuizResponse>> generate(
            @Valid @RequestBody GenerateQuizRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(quizService.generateQuiz(request)));
    }

    /** Submit a single answer for a question. Returns correct/incorrect + explanation. */
    @PostMapping("/questions/{questionId}/answer")
    public ResponseEntity<ApiResponse<AnswerResultResponse>> submitAnswer(
            @PathVariable UUID questionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(quizService.submitAnswer(questionId, request)));
    }

    /** Retrieve a quiz by ID (with questions — no correct answers revealed). */
    @GetMapping("/{quizId}")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuiz(@PathVariable UUID quizId) {
        return ResponseEntity.ok(ApiResponse.ok(quizService.getQuiz(quizId)));
    }

    /** Full quiz history for a user. */
    @GetMapping("/history/{userId}")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getHistory(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(quizService.getHistory(userId)));
    }
}
