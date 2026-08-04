package com.guruai.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guruai.common.security.InternalAccessProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AgentConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * Classic Jackson 2 ObjectMapper — Spring Boot 4 defaults to Jackson 3
     * and doesn't auto-configure this bean (same issue as quiz/flashcard/
     * user-memory-service's JacksonConfig). Needed here specifically to
     * hand-wire Jackson2 codecs onto webClientBuilder below.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * All service-to-service calls go through this builder, which stamps the
     * internal secret on every outgoing request. Without it, the target
     * service's InternalAccessFilter would reject us with a 403 the same way
     * it rejects any other caller that didn't come through the gateway —
     * the filter can't tell "sibling service" from "attacker" except by
     * this header.
     *
     * <p>The explicit Jackson2 codecs matter too: because this method builds
     * a fresh {@code WebClient.builder()} instead of injecting Boot's
     * auto-configured builder bean, it skips Boot's automatic JSON codec
     * wiring — WebClient falls back to its bare default codecs, which are
     * Jackson-3-based in Boot 4 and cannot construct a classic
     * {@code com.fasterxml.jackson.databind.JsonNode} (DocumentServiceClient
     * and KnowledgeServiceClient both call {@code .bodyToMono(JsonNode.class)}).
     * Without this, every WebClient call for those types failed with
     * "Type definition error: [simple type, class ...JsonNode]" and was
     * silently swallowed by the clients' try/catch, degrading every chat
     * turn to "no context found" instead of an actual error.
     */
    @Bean
    public WebClient.Builder webClientBuilder(InternalAccessProperties internalProps,
                                               ObjectMapper objectMapper) {
        return WebClient.builder()
                .defaultHeader("X-Internal-Secret", internalProps.secret())
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                });
    }
}
