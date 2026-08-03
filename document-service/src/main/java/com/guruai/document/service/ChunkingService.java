package com.guruai.document.service;

import java.util.List;

/**
 * Contract for text chunking (splitting large documents into retrieval-sized pieces).
 *
 * <p>Ported from Python {@code chunker.py} — 1000 char / 150 overlap sliding window
 * with sentence-boundary awareness.
 *
 * <p>Implemented by {@link com.guruai.document.service.impl.ChunkingServiceImpl}.
 */
public interface ChunkingService {

    /**
     * Split a document's plain text into overlapping chunks for embedding.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>If {@code text.length() ≤ chunkSize} → return single chunk</li>
     *   <li>Otherwise, slide a window of {@code chunkSize} characters</li>
     *   <li>At each boundary, find the nearest sentence boundary (period/newline)
     *       that is at least half-way through the window</li>
     *   <li>Step back by {@code chunkOverlap} for the next window start</li>
     * </ol>
     *
     * @param text full plain-text content of the document
     * @return ordered list of non-empty chunk strings
     */
    List<String> chunk(String text);

    /**
     * Chunk with explicit size and overlap parameters (overrides application.yml defaults).
     *
     * @param text         full document text
     * @param chunkSize    maximum characters per chunk
     * @param chunkOverlap characters of overlap between adjacent chunks
     * @return ordered list of non-empty chunk strings
     */
    List<String> chunk(String text, int chunkSize, int chunkOverlap);
}
