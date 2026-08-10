package com.guruai.quiz.repository;

import com.guruai.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    List<Quiz> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Quiz> findByUserIdAndSubjectOrderByCreatedAtDesc(UUID userId, String subject);
}
