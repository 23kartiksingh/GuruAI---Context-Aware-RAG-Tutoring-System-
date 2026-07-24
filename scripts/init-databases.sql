-- ============================================================
-- GuruAI — Database Initializer (single-instance layout)
-- Run automatically by Docker on the postgres container's FIRST
-- startup with an empty volume (docker-entrypoint-initdb.d).
-- ============================================================
-- One Postgres INSTANCE hosts eight isolated DATABASES — one per
-- service, which is what "database per service" actually requires
-- (each service can only see its own schema/tables). Running eight
-- separate server instances would buy independent scaling/failure
-- isolation, which matters in production but just burns ~500MB of
-- RAM on a dev laptop. Production would split these out again
-- without any code change — services only know a JDBC URL.
--
-- POSTGRES_DB creates auth_db; this script creates the other seven.
-- ============================================================

CREATE DATABASE document_db;
CREATE DATABASE agent_db;
CREATE DATABASE knowledge_db;
CREATE DATABASE quiz_db;
CREATE DATABASE flashcard_db;
CREATE DATABASE memory_db;
CREATE DATABASE notif_db;

-- Enable pgvector where it's needed: document_db stores the
-- 768-dimensional Gemini embeddings. (The pgvector/pgvector image
-- has the extension compiled in; it just needs activating in the
-- right database.)
\connect document_db
CREATE EXTENSION IF NOT EXISTS vector;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        RAISE NOTICE '✅ pgvector extension is active in document_db';
    ELSE
        RAISE EXCEPTION '❌ pgvector extension failed to load!';
    END IF;
END $$;
