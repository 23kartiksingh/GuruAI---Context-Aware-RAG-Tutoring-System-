-- ================================================================
-- Knowledge Service — Flyway V2 Migration
-- Scopes mastery and subject enrolment to a study session
-- ================================================================
-- Mastery used to be keyed on (user, subject, topic), which meant a single
-- shared row no matter which session produced the signal. Deleting a session
-- therefore couldn't remove the knowledge built up in it, and studying the
-- same subject in two sessions blended into one score.
--
-- Adding session_id makes mastery per-session: the same topic tracked in two
-- sessions now has two independent EMA scores, and session deletion can drop
-- exactly the rows that session created.
--
-- Existing rows predate sessions, so session_id is nullable rather than
-- backfilled with a fabricated ID. Postgres treats NULLs as distinct in a
-- UNIQUE constraint, so those legacy rows are left alone and simply never
-- match a session-scoped lookup — new signals create fresh session-scoped
-- rows alongside them.

ALTER TABLE topic_mastery  ADD COLUMN session_id UUID;
ALTER TABLE user_subjects  ADD COLUMN session_id UUID;

-- Swap the uniqueness rules to include the session.
ALTER TABLE topic_mastery DROP CONSTRAINT uq_user_subject_topic;
ALTER TABLE topic_mastery
    ADD CONSTRAINT uq_user_session_subject_topic
    UNIQUE (user_id, session_id, subject, topic);

ALTER TABLE user_subjects DROP CONSTRAINT uq_user_subject;
ALTER TABLE user_subjects
    ADD CONSTRAINT uq_user_session_subject
    UNIQUE (user_id, session_id, subject);

-- Deleting a session filters on these.
CREATE INDEX idx_topic_mastery_session ON topic_mastery(session_id);
CREATE INDEX idx_user_subjects_session ON user_subjects(session_id);
