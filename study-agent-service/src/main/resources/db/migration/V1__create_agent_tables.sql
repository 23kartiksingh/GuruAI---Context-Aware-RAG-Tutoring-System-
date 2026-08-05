-- ================================================================
-- Study Agent Service — Flyway V1 Migration
-- Creates: sessions, messages
-- ================================================================

CREATE TABLE sessions (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    title      VARCHAR(300) NOT NULL,
    subject    VARCHAR(200),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);

CREATE TABLE messages (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID         NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    role       VARCHAR(20)  NOT NULL,
    content    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_session_id ON messages(session_id);
CREATE INDEX idx_messages_created    ON messages(session_id, created_at ASC);
