package com.guruai.quiz.repository;

import com.guruai.quiz.entity.QuestionRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionRefRepository extends JpaRepository<QuestionRef, UUID> {

    List<QuestionRef> findByQuizId(UUID quizId);
}
