package com.guruai.quiz.service;

import com.guruai.quiz.dto.request.GenerateQuizRequest;
import com.guruai.quiz.dto.request.SubmitAnswerRequest;
import com.guruai.quiz.dto.response.AnswerResultResponse;
import com.guruai.quiz.dto.response.QuizResponse;

import java.util.List;
import java.util.UUID;

public interface QuizService {

    QuizResponse generateQuiz(GenerateQuizRequest request);

    AnswerResultResponse submitAnswer(UUID questionId, SubmitAnswerRequest request);

    QuizResponse getQuiz(UUID quizId);

    List<QuizResponse> getHistory(UUID userId);
}
