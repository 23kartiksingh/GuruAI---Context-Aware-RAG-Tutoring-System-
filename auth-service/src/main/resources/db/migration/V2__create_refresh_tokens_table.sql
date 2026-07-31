-- ============================================================
-- V2 — Create Refresh Tokens Table
-- Auth Service | auth_db
-- ============================================================

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      TEXT        UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Look up a token quickly for refresh / revocation
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token   ON refresh_tokens (token);

-- Delete all tokens for a user on logout-all-devices
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens (user_id);

COMMENT ON TABLE  refresh_tokens           IS 'Long-lived refresh tokens (14 day TTL)';
COMMENT ON COLUMN refresh_tokens.revoked   IS 'TRUE when explicitly revoked (logout). Expired rows cleaned by nightly cron.';
