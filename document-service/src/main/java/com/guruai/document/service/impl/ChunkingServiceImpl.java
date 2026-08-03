package com.guruai.document.service.impl;

import com.guruai.document.config.DocumentProperties;
import com.guruai.document.service.ChunkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link ChunkingService}.
 *
 * <p>Direct Java port of the Python sliding-window chunker in {@code chunker.py}:
 * <pre>
 *   chunk_size    = 1000  characters
 *   chunk_overlap = 150   characters
 * </pre>
 *
 * <p>Sentence-boundary awareness: at each chunk boundary the algorithm looks backward
 * for the nearest sentence-ending character (period, newline) that is at least
 * half-way through the window. This prevents mid-sentence cuts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkingServiceImpl implements ChunkingService {

    private final DocumentProperties props;

    // ── Public API ─────────────────────────────────────────────────────────────

    @Override
    public List<String> chunk(String text) {
        int size    = props.document().chunkSize();
        int overlap = props.document().chunkOverlap();
        return chunk(text, size, overlap);
    }

    @Override
    public List<String> chunk(String text, int chunkSize, int chunkOverlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String cleaned = text.strip();

        // Short documents: return as single chunk
        if (cleaned.length() <= chunkSize) {
            return List.of(cleaned);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < cleaned.length()) {
            int end = Math.min(start + chunkSize, cleaned.length());

            // ── Sentence-boundary snapping ─────────────────────────────────
            // Only snap if we're not at the very end of the document
            if (end < cleaned.length()) {
                int halfWay = start + chunkSize / 2;

                // Look backward for '. ' or '\n' in [halfWay, end]
                int lastPeriod  = lastIndexOf(cleaned, '.', halfWay, end);
                int lastNewline = lastIndexOf(cleaned, '\n', halfWay, end);
                int boundary    = Math.max(lastPeriod, lastNewline);

                if (boundary > halfWay) {
                    end = boundary + 1;   // include the boundary character
                }
            }

            String chunk = cleaned.substring(start, end).strip();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // We just consumed the rest of the document — stop here.
            // Without this check, once the window reaches the tail (end
            // pinned at cleaned.length() because Math.min caps it, and the
            // sentence-snap block above is skipped whenever end == length),
            // "start = end - chunkOverlap" recomputes to the SAME value
            // every iteration — a fixed point the loop can never escape.
            // That's an infinite loop that keeps re-adding the identical
            // tail chunk forever and OOMs on ANY text longer than
            // chunkSize, independent of the source file's size (this is
            // what was actually happening, not a Tika/memory sizing issue).
            if (end >= cleaned.length()) {
                break;
            }

            // Slide window with overlap
            start = end - chunkOverlap;
        }

        log.debug("Chunked {} chars → {} chunks (size={}, overlap={})",
                  cleaned.length(), chunks.size(), chunkSize, chunkOverlap);
        return chunks;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Find the last occurrence of {@code ch} in {@code text[from..to)}.
     *
     * @return index of the last occurrence, or -1 if not found in range
     */
    private int lastIndexOf(String text, char ch, int from, int to) {
        for (int i = Math.min(to, text.length()) - 1; i >= from; i--) {
            if (text.charAt(i) == ch) return i;
        }
        return -1;
    }
}
