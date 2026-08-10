package com.guruai.quiz.entity;

import com.guruai.common.enums.DifficultyLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a quiz attempt by a student.
 * Linked to a session and constrained to the user's enrolled subjects.
 */
@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "topic", length = 300)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private DifficultyLevel difficulty = DifficultyLevel.INTERMEDIATE;

    /** Overall score percentage (0–100). Null until quiz is submitted. */
    @Column(name = "score_pct")
    private Integer scorePct;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "correct_answers")
    private int correctAnswers;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Quiz(UUID userId, UUID sessionId, String subject, String topic,
                DifficultyLevel difficulty, int totalQuestions) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.subject = subject;
        this.topic = topic;
        this.difficulty = difficulty;
        this.totalQuestions = totalQuestions;
    }
}
