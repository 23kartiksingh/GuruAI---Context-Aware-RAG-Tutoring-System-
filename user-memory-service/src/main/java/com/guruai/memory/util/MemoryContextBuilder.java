package com.guruai.memory.util;

import java.util.List;
import java.util.stream.Collectors;

/** Formats a user's memory items into a concise system prompt context block. */
public final class MemoryContextBuilder {

    private MemoryContextBuilder() {}

    /**
     * Builds a formatted context string for injection into the Study Agent system prompt.
     *
     * @param items list of memory item strings
     * @return formatted string, or empty string if no items
     */
    public static String buildContext(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return "About this student — use these specific details (by name) in examples and analogies whenever relevant:\n" +
                items.stream()
                        .map(item -> "- " + item)
                        .collect(Collectors.joining("\n"));
    }
}
