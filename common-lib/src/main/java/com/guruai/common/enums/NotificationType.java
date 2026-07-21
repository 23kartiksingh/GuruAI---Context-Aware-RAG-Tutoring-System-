package com.guruai.common.enums;

/**
 * Categories of notifications that GuruAI can push to the user.
 *
 * <p>Used by Notification Service to:
 * <ul>
 *   <li>Store notifications in notif_db with the correct type</li>
 *   <li>Allow users to configure per-type preferences (opt-in/out)</li>
 *   <li>Filter WebSocket pushes</li>
 * </ul>
 */
public enum NotificationType {

    /** A user registered successfully — welcome message. */
    WELCOME,

    /**
     * A topic's EMA mastery score dropped from AVERAGE → WEAK.
     * Triggered by {@code mastery.dropped} Kafka event.
     */
    MASTERY_DROP,

    /** User completed a quiz — streak updated or congrats message. */
    QUIZ_STREAK,

    /** Flashcards are due for review today (daily nudge). */
    FLASHCARD_DUE,

    /** A new learning path was generated or updated. */
    LEARNING_PATH_UPDATED,

    /** Document was indexed and flashcards were auto-generated. */
    DOCUMENT_PROCESSED,

    /** Generic informational message. */
    INFO
}
