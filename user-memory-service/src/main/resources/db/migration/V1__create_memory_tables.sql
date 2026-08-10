-- ================================================================
-- User Memory Service — Flyway V1 Migration
-- Creates: user_memories, memory_chat_history
-- ================================================================

CREATE TABLE user_memories (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    item       TEXT         NOT NULL,
    item_hash  VARCHAR(64)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_memory_item UNIQUE (user_id, item_hash)
);

CREATE INDEX idx_user_memories_user_id ON user_memories(user_id);

CREATE TABLE memory_chat_history (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    content    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_memory_chat_user_id ON memory_chat_history(user_id);
CREATE INDEX idx_memory_chat_created ON memory_chat_history(user_id, created_at DESC);
