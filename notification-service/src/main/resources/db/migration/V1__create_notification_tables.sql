-- ================================================================
-- Notification Service — Flyway V1 Migration
-- Creates: notifications
-- ================================================================

CREATE TABLE notifications (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    type       VARCHAR(30)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    message    TEXT         NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id      ON notifications(user_id);
CREATE INDEX idx_notifications_user_unread  ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_created      ON notifications(user_id, created_at DESC);
