package com.guruai.common.events;

import com.guruai.common.enums.DifficultyLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event — published by Quiz Service on topic {@code quiz.completed}.
 *
 * <p>Despite the topic name, this fires once per ANSWERED QUESTION, not once
 * per finished quiz — {@code isCorrect} is always that one question's result.
 * An earlier version gated publishing behind "all questions answered," which
 * meant only the last question of any quiz ever reached Knowledge Service;
 * every earlier answer in the same attempt was silently discarded from
 * mastery tracking. {@code quizComplete} is what distinguishes "the whole
 * attempt just finished" from "one more question was answered," since both
 * consumers below need different semantics from the same stream.
 *
 * <p>Consumers:
 * <ul>
 *   <li><b>Knowledge Service</b> — runs one EMA update per event (i.e. per
 *       question), regardless of {@code quizComplete}</li>
 *   <li><b>Notification Service</b> — ignores events where
 *       {@code quizComplete} is false; sends its streak/encouragement
 *       notification only once, on the event where it's true</li>
 * </ul>
 *
 * @param eventId      unique event ID
 * @param quizId       UUID of the quiz attempt row
 * @param userId       the student
 * @param sessionId    session the quiz was generated from
 * @param subject      subject the quiz was generated for (constrained by user subjects)
 * @param topic        specific topic this quiz tested
 * @param isCorrect    whether THIS ONE question's answer was correct
 * @param difficulty   {@link DifficultyLevel} the quiz was generated at
 * @param totalScore   running score percentage (0–100) so far in the attempt;
 *                     only meaningful/final once {@code quizComplete} is true
 * @param quizComplete true only on the event for the last question answered
 * @param occurredAt   when the answer was submitted
 */
public record QuizCompletedEvent(
        String          eventId,
        String          quizId,
        String          userId,
        String          sessionId,
        String          subject,
        String          topic,
        boolean         isCorrect,
        DifficultyLevel difficulty,
        int             totalScore,
        boolean         quizComplete,
        Instant         occurredAt
) {
    public static QuizCompletedEvent of(
            String quizId, String userId, String sessionId,
            String subject, String topic, boolean isCorrect,
            DifficultyLevel difficulty, int totalScore, boolean quizComplete) {
        return new QuizCompletedEvent(
                UUID.randomUUID().toString(),
                quizId, userId, sessionId,
                subject, topic, isCorrect,
                difficulty, totalScore, quizComplete,
                Instant.now()
        );
    }
}
