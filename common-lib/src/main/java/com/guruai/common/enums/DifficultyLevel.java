package com.guruai.common.enums;

/**
 * Difficulty level for generated quizzes and flashcards.
 *
 * <p>Maps directly to the student's {@link MasteryLevel} for the topic
 * being tested — Quiz Service uses this to prompt the AI accordingly.
 *
 * <ul>
 *   <li>{@code BEGINNER} ← WEAK mastery: core definitions, basic concepts</li>
 *   <li>{@code INTERMEDIATE} ← AVERAGE mastery: mixed foundational + intermediate</li>
 *   <li>{@code ADVANCED} ← STRONG mastery: edge cases, deep theory, applications</li>
 * </ul>
 */
public enum DifficultyLevel {

    /** Mirrors WEAK mastery. Focus on fundamentals and definitions. */
    BEGINNER,

    /** Mirrors AVERAGE mastery. Mix of foundational and intermediate. */
    INTERMEDIATE,

    /** Mirrors STRONG mastery. Edge cases, advanced theory, applications. */
    ADVANCED;

    /**
     * Derive the appropriate difficulty from a student's mastery level.
     *
     * @param masteryLevel the student's current {@link MasteryLevel}
     * @return the corresponding {@link DifficultyLevel}
     */
    public static DifficultyLevel from(MasteryLevel masteryLevel) {
        return switch (masteryLevel) {
            case WEAK    -> BEGINNER;
            case AVERAGE -> INTERMEDIATE;
            case STRONG  -> ADVANCED;
        };
    }
}
