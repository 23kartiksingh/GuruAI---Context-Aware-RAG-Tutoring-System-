package com.guruai.quiz.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores the text and correct answer for a quiz question (Question-ID pattern).
 * The correct answer is never sent to the client — only referenced on submission.
 */
@Entity
@Table(name = "question_refs")
@Getter
@Setter
@NoArgsConstructor
public class QuestionRef {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /** Options as a JSON array string: ["A) ...", "B) ...", "C) ...", "D) ..."] */
    @Column(name = "options_json", nullable = false, columnDefinition = "TEXT")
    private String optionsJson;

    /** The letter of the correct option (e.g., "A", "B", "C", "D"). */
    @Column(name = "correct_answer", nullable = false, length = 1)
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "topic", length = 300)
    private String topic;

    /** The user's submitted answer — null until answered. */
    @Column(name = "user_answer", length = 1)
    private String userAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
