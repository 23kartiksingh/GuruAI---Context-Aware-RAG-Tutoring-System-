-- ================================================================
-- Flashcard Service — Flyway V1 Migration
-- Creates: flashcards
-- ================================================================

CREATE TABLE flashcards (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL,
    session_id       UUID,
    subject          VARCHAR(200),
    topic            VARCHAR(300),
    front            TEXT         NOT NULL,
    back             TEXT         NOT NULL,

    -- SM-2 algorithm state
    ease_factor      DOUBLE PRECISION NOT NULL DEFAULT 2.5,
    interval_days    INT          NOT NULL DEFAULT 1,
    repetitions      INT          NOT NULL DEFAULT 0,
    next_review_date DATE         NOT NULL DEFAULT CURRENT_DATE,

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_flashcards_user_id           ON flashcards(user_id);
CREATE INDEX idx_flashcards_user_review_date  ON flashcards(user_id, next_review_date);
CREATE INDEX idx_flashcards_session_id        ON flashcards(session_id);
