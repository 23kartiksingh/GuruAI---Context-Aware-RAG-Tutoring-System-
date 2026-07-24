-- ============================================================
-- pgvector Extension Initializer
-- Run automatically by Docker on document-db first startup
-- ============================================================
-- Enables the pgvector extension so we can store and query
-- 768-dimensional Gemini embedding vectors directly in PostgreSQL.
-- The pgvector/pgvector:pg16 image has the extension compiled in,
-- we just need to activate it in the target database.

CREATE EXTENSION IF NOT EXISTS vector;

-- Verify it loaded
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        RAISE NOTICE '✅ pgvector extension is active in document_db';
    ELSE
        RAISE EXCEPTION '❌ pgvector extension failed to load!';
    END IF;
END $$;
