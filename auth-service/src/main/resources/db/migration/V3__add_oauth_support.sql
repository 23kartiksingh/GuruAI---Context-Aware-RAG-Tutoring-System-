-- ============================================================
-- V3 — Add Google OAuth2 support
-- Auth Service | auth_db
-- ============================================================
-- A Google-created account has no password, so password_hash must
-- become nullable. email/auth_provider/provider_id are new — existing
-- rows default to auth_provider='LOCAL' with no email on file (register
-- never collected one), which is fine since Google login matches on
-- (auth_provider, provider_id), never on email.
-- ============================================================

ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email         VARCHAR(255),
    ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN IF NOT EXISTS provider_id   VARCHAR(255);

-- One Google account can only ever back one GuruAI user.
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_provider
    ON users (auth_provider, provider_id)
    WHERE provider_id IS NOT NULL;

COMMENT ON COLUMN users.password_hash  IS 'BCrypt-hashed password. NULL for Google-only accounts.';
COMMENT ON COLUMN users.email          IS 'Verified email from the OAuth2 provider. NULL for LOCAL accounts (register never asks for one).';
COMMENT ON COLUMN users.auth_provider  IS 'LOCAL (username/password) or GOOGLE.';
COMMENT ON COLUMN users.provider_id    IS 'Provider''s stable subject id (Google''s "sub" claim). NULL for LOCAL accounts.';
