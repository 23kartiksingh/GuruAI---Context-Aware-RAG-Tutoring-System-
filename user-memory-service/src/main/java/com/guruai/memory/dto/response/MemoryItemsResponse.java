package com.guruai.memory.dto.response;

import java.util.List;

public record MemoryItemsResponse(
        List<MemoryItemResponse> items,
        int count
) {}
