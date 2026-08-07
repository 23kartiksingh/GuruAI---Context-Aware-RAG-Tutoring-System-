package com.guruai.knowledge.event.consumer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.ChatMessageSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code chat.message.saved} from study-agent-service.
 *
 * <p>What this deliberately does NOT do: update EMA mastery scores from chat
 * content. The other mastery signals (quiz.completed, flashcard.reviewed)
 * carry an explicit correct/incorrect flag; a chat message doesn't. Deriving
 * "did the student demonstrate understanding?" from free-form chat text would
 * need an LLM grading call — and knowledge-service intentionally has no AI
 * dependency (it's the one service whose numbers should be deterministic and
 * explainable: pure EMA math over explicit signals).
 *
 * <p>So chat events are consumed as an <b>activity signal only</b> (logged,
 * available for future streak/engagement features in notification-service's
 * domain). If chat-derived mastery is ever wanted, the right design is for
 * study-agent-service (which already has the LLM) to grade the exchange and
 * publish a quiz.completed-style event with an explicit correctness flag —
 * not for this service to grow an AI client.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChatMessageSavedConsumer {

    @KafkaListener(topics = KafkaTopics.CHAT_MESSAGE_SAVED, groupId = "knowledge-service-group")
    public void onChatMessageSaved(ChatMessageSavedEvent event) {
        // Activity signal only — see class javadoc for why there's no EMA update here.
        log.debug("Chat activity: userId={} sessionId={} role={} chars={}",
                event.userId(), event.sessionId(), event.role(),
                event.content() == null ? 0 : event.content().length());
    }
}
