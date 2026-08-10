-- ================================================================
-- Knowledge Service — Flyway V1 Migration
-- Creates: user_subjects, topic_mastery
-- ================================================================

CREATE TABLE user_subjects (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    subject     VARCHAR(200) NOT NULL,
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT  uq_user_subject UNIQUE (user_id, subject)
);

CREATE INDEX idx_user_subjects_user_id ON user_subjects(user_id);

CREATE TABLE topic_mastery (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID          NOT NULL,
    subject       VARCHAR(200)  NOT NULL,
    topic         VARCHAR(300)  NOT NULL,
    ema_score     DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    correct_count INT           NOT NULL DEFAULT 0,
    total_count   INT           NOT NULL DEFAULT 0,
    mastery_level VARCHAR(20)   NOT NULL DEFAULT 'AVERAGE',
    last_updated  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT    uq_user_subject_topic UNIQUE (user_id, subject, topic)
);

CREATE INDEX idx_topic_mastery_user_id      ON topic_mastery(user_id);
CREATE INDEX idx_topic_mastery_user_subject ON topic_mastery(user_id, subject);
CREATE INDEX idx_topic_mastery_level        ON topic_mastery(user_id, mastery_level);
