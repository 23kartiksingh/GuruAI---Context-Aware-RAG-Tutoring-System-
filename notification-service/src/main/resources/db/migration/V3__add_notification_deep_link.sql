-- Lets a notification deep-link into a specific session/topic (currently
-- only WEAK_TOPIC_REMINDER populates these; every other notification type
-- leaves them null). Nullable because most existing and future notification
-- types have nothing to link to.
ALTER TABLE notifications ADD COLUMN session_id UUID;
ALTER TABLE notifications ADD COLUMN topic VARCHAR(200);
