-- ============================================================
-- V2 — Create vector_store table (Spring AI pgvector schema)
-- Document Service | document_db
-- ============================================================
-- We manage the Spring AI vector_store table via Flyway so we can:
-- 1. Add the HNSW index for ANN (Approximate Nearest Neighbour) search
-- 2. Add a GIN full-text index for BM25-style keyword search
-- 3. Set initialize-schema=false in Spring AI (Flyway owns it)
--
-- Spring AI stores per-chunk metadata in the 'metadata' JSON column:
-- {
--   "document_id": "...",
--   "session_id":  "...",
--   "user_id":     "...",
--   "chunk_index": 0,
--   "filename":    "chapter3.pdf",
--   "topic":       "Sorting Algorithms"
-- }
-- ============================================================

-- Enable pgvector extension (idempotent — script in init-pgvector.sql also does this)
CREATE EXTENSION IF NOT EXISTS vector;

-- Spring AI vector_store table
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT,                    -- raw chunk text
    metadata  JSON,                    -- per-chunk metadata (document_id, session_id, etc.)
    embedding vector(768)              -- gemini-embedding-001 produces 768-dim vectors
);

-- ── HNSW index for fast ANN cosine similarity search ─────────────────────────
-- m=16: number of connections per node (higher=better recall, more memory)
-- ef_construction=64: build-time candidates (higher=better quality, slower build)
CREATE INDEX IF NOT EXISTS spring_ai_vector_hnsw_idx
    ON vector_store
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- ── GIN index for BM25-style full-text keyword search ────────────────────────
-- Enables fast to_tsquery() searches on the 'content' column
CREATE INDEX IF NOT EXISTS vector_store_fts_idx
    ON vector_store
    USING GIN (to_tsvector('english', COALESCE(content, '')));

-- ── Index on metadata JSON for fast session-scoped retrieval ─────────────────
-- The column is JSON (Spring AI's PgVectorStore writes it as json), but the
-- jsonb_path_ops operator class only accepts JSONB — indexing the bare
-- column fails with "operator class jsonb_path_ops does not accept data
-- type json" and kills the whole migration. So this is an EXPRESSION index
-- over a jsonb cast instead; queries that filter via metadata::jsonb
-- (see HybridSearchServiceImpl) can use it.
CREATE INDEX IF NOT EXISTS vector_store_metadata_session_idx
    ON vector_store
    USING GIN ((metadata::jsonb) jsonb_path_ops);

COMMENT ON TABLE  vector_store            IS 'Spring AI pgvector chunk embeddings store. Managed by Flyway.';
COMMENT ON COLUMN vector_store.embedding  IS '768-dimensional Gemini text-embedding-004 vector';
COMMENT ON COLUMN vector_store.metadata   IS 'JSON: {document_id, session_id, user_id, chunk_index, filename, topic}';
