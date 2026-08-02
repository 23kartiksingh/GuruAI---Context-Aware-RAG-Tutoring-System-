package com.guruai.document.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI auto-configures a {@link ChatClient.Builder} bean, but not a
 * ready-to-use {@link ChatClient} — that last {@code .build()} step is left
 * to the application. DocumentIndexingWorker injects ChatClient directly
 * (for Gemini-based topic extraction), so without this bean the app fails
 * at startup with "No qualifying bean of type ChatClient".
 * (Same pattern as study-agent-service's AgentConfig.)
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
