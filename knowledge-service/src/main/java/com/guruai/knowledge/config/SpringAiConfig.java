package com.guruai.knowledge.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI auto-configures a {@link ChatClient.Builder} bean from
 * {@code spring.ai.openai.*} properties, but NOT a {@link ChatClient} itself
 * — the application has to call {@code .build()} on it. Without this,
 * {@link com.guruai.knowledge.service.impl.TopicCanonicalizerService}'s
 * constructor (which wants a {@code ChatClient} directly) fails to resolve
 * at startup with "No qualifying bean of type ChatClient", crash-looping the
 * whole service — every downstream endpoint (including plain CRUD ones like
 * {@code /knowledge/{userId}/stats}) then fails too, since the app never
 * finishes starting. Same one-liner as study-agent-service's AgentConfig and
 * user-memory-service's SpringAiConfig.
 */
@Configuration
public class SpringAiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
