package com.guruai.quiz.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the classic Jackson 2 {@link ObjectMapper} as a bean.
 *
 * <p>Why this exists: Spring Boot 4 switched its default JSON engine to
 * Jackson 3 ({@code tools.jackson}), so it no longer auto-configures a
 * {@code com.fasterxml.jackson.databind.ObjectMapper} bean. Our services
 * inject the classic ObjectMapper for parsing LLM responses and question
 * JSON — without this bean the application fails at startup with
 * "No qualifying bean of type ObjectMapper" and crash-loops.
 * (Same config exists in flashcard-service and user-memory-service.)
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                // LLM JSON often has extra fields we don't model — don't explode.
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
