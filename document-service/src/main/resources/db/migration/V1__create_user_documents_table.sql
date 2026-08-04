-- ============================================================
-- V1 — Create Documents Table
-- Document Service | document_db
-- ============================================================

CREATE TABLE IF NOT EXISTS user_documents (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID            NOT NULL,
    user_id         UUID            NOT NULL,
    filename        VARCHAR(500)    NOT NULL,
    file_type       VARCHAR(50)     NOT NULL,           -- pdf | docx | txt | image
    file_size_bytes BIGINT          NOT NULL DEFAULT 0,
    chunk_count     INT             NOT NULL DEFAULT 0,
    status          VARCHAR(50)     NOT NULL DEFAULT 'PROCESSING',  -- PROCESSING | INDEXED | FAILED
    topics          JSONB           NOT NULL DEFAULT '[]',          -- AI-extracted topic list
    subject         VARCHAR(200),                                   -- primary detected subject
    error_message   TEXT,                              -- populated if status=FAILED
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_documents_session_id ON user_documents (session_id);
CREATE INDEX IF NOT EXISTS idx_user_documents_user_id    ON user_documents (user_id);
CREATE INDEX IF NOT EXISTS idx_user_documents_status     ON user_documents (status);
CREATE INDEX IF NOT EXISTS idx_user_documents_topics     ON user_documents USING GIN (topics);

COMMENT ON TABLE  user_documents                IS 'Metadata for uploaded study documents';
COMMENT ON COLUMN user_documents.topics         IS 'JSON array of extracted topic strings, e.g. ["Sorting Algorithms","Big-O Notation"]';
COMMENT ON COLUMN user_documents.chunk_count    IS 'Number of vector chunks stored in Spring AI vector_store';
