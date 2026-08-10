package com.guruai.memory.service;

import java.util.List;

public interface PreferenceExtractorService {
    /**
     * Uses Groq to extract structured preference items from freeform user text.
     * Returns a list of concise preference strings (max 15 words each).
     */
    List<String> extract(String userText);
}
