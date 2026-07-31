-- ============================================================
-- V1 — Create Users Table
-- Auth Service | auth_db
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(200) NOT NULL DEFAULT 'The Scholar',
    bio           TEXT         NOT NULL DEFAULT '',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Fast lookup by username (login)
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);

-- Comments
COMMENT ON TABLE  users                IS 'Registered GuruAI users';
COMMENT ON COLUMN users.username       IS 'Unique lowercase login name';
COMMENT ON COLUMN users.password_hash  IS 'BCrypt-hashed password';
COMMENT ON COLUMN users.name           IS 'Display name shown in UI';
COMMENT ON COLUMN users.bio            IS 'Short user bio / learning goals';
