package com.guruai.document.entity;

/**
 * Processing status of an uploaded document.
 *
 * <p>Lifecycle:
 * <pre>
 *   PROCESSING → INDEXED   (success)
 *   PROCESSING → FAILED    (Tika parse error, embedding error, etc.)
 * </pre>
 */
public enum DocumentStatus {

    /** Document received and is being parsed, chunked, and embedded. */
    PROCESSING,

    /** All chunks have been embedded and stored in pgvector. Ready for search. */
    INDEXED,

    /** Processing failed — see {@code error_message} column for details. */
    FAILED
}
