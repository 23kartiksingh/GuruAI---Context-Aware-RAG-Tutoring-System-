package com.guruai.common.constants;

/**
 * Central registry of all Kafka topic names used across GuruAI microservices.
 *
 * <p>NEVER hard-code topic names in individual services — always reference these constants.
 * This ensures every service agrees on the exact topic string.
 *
 * <p>Topic naming convention: {@code <domain>.<event>} (all lowercase, dot-separated)
 */
public final class KafkaTopics {

    private KafkaTopics() { /* utility class — no instances */ }

    // ── Auth Domain ──────────────────────────────────────────────────────────
    /** Fired when a new user successfully registers. */
    public static final String USER_REGISTERED        = "user.registered";

    // ── Document Domain ──────────────────────────────────────────────────────
    /**
     * Fired when a document has been fully parsed, chunked, embedded, and
     * stored in pgvector. Carries auto-extracted topic list.
     */
    public static final String DOCUMENT_INDEXED       = "document.indexed";

    // ── Chat Domain ──────────────────────────────────────────────────────────
    /** Fired each time a message (user or assistant) is persisted to chat_db. */
    public static final String CHAT_MESSAGE_SAVED     = "chat.message.saved";

    // ── Quiz Domain ──────────────────────────────────────────────────────────
    /** Fired when a user completes (submits) a quiz attempt. */
    public static final String QUIZ_COMPLETED         = "quiz.completed";

    // ── Flashcard Domain ─────────────────────────────────────────────────────
    /** Fired when a user reviews a flashcard and submits a quality score (0–5). */
    public static final String FLASHCARD_REVIEWED     = "flashcard.reviewed";

    // ── Session Domain ───────────────────────────────────────────────────────
    /**
     * Fired when a study session is deleted. Each service owns the cleanup of
     * its own session-scoped data: document-service drops the documents and
     * their pgvector chunks, flashcard-service drops the generated cards, and
     * knowledge-service drops the mastery recorded in that session.
     */
    public static final String SESSION_DELETED        = "session.deleted";

    // ── Knowledge Domain ─────────────────────────────────────────────────────
    /**
     * Fired by Knowledge Service when a topic's EMA score drops below the
     * AVERAGE threshold (0.5). Triggers a notification to the user.
     */
    public static final String MASTERY_DROPPED        = "mastery.dropped";

    /**
     * Fired periodically (every ~10 minutes, subject to a per-user cooldown)
     * by Knowledge Service for students who still have a weak topic tracked
     * against a live session — a recurring nudge, unlike the one-off
     * MASTERY_DROPPED alert.
     */
    public static final String WEAK_TOPIC_REMINDER    = "weak.topic.reminder";

    // NOTE: an earlier design also had a "notification.trigger" topic for
    // notification-service to re-broadcast events internally. It was never
    // produced or consumed by anything — notification-service consumes the
    // domain topics above directly and pushes over WebSocket — so it was
    // removed rather than left as dead infrastructure.

    // ── Topic List (for programmatic registration / admin) ───────────────────
    public static final String[] ALL_TOPICS = {
        USER_REGISTERED,
        DOCUMENT_INDEXED,
        CHAT_MESSAGE_SAVED,
        QUIZ_COMPLETED,
        FLASHCARD_REVIEWED,
        MASTERY_DROPPED,
        SESSION_DELETED,
        WEAK_TOPIC_REMINDER
    };
}
