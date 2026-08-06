package com.guruai.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code guruai.mastery.*} YAML properties.
 */
@ConfigurationProperties(prefix = "guruai.mastery")
public record MasteryProperties(
        double emaAlpha,
        double weakThreshold,
        double strongThreshold
) {}
