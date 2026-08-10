package com.guruai.quiz.service;

import com.guruai.quiz.dto.request.GenerateQuizRequest;
import com.guruai.quiz.entity.QuestionRef;

import java.util.List;

public interface QuizGeneratorService {
    List<QuestionRef> generate(GenerateQuizRequest request);
}
