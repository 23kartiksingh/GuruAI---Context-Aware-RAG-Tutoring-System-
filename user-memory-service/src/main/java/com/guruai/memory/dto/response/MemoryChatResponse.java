package com.guruai.memory.dto.response;

import java.util.List;

public record MemoryChatResponse(
        String reply,
        List<String> currentMemoryItems
) {}
