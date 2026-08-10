-- ================================================================
-- Notification Service — Flyway V2 Migration
-- Adds: notifications.event_id (idempotency key)
-- ================================================================
-- Kafka guarantees at-least-once delivery, not exactly-once: a consumer
-- can legitimately see the same event twice (consumer-group rebalance,
-- offset reset, or a redelivery after a failed commit). With
-- auto-offset-reset=earliest, a group that loses its committed offset
-- replays the whole topic from the beginning — which is what produced
-- six identical "Quiz completed" rows for a single quiz.
--
-- Every event record in common-lib already carries a unique eventId, so
-- storing it here and enforcing uniqueness turns a replay into a no-op.
--
-- NULLable on purpose: rows created before this migration have no event
-- ID, and Postgres allows unlimited NULLs in a UNIQUE index — so the
-- constraint applies to new rows without a backfill.

ALTER TABLE notifications ADD COLUMN event_id VARCHAR(64);

CREATE UNIQUE INDEX uq_notifications_event_id
    ON notifications(event_id)
    WHERE event_id IS NOT NULL;
