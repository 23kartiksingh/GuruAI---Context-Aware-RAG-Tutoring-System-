package com.guruai.knowledge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddSubjectRequest(
        @NotBlank(message = "Subject must not be blank")
        @Size(max = 200, message = "Subject must be at most 200 characters")
        String subject
) {}
