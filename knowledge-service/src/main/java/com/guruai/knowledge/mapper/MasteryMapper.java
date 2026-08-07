package com.guruai.knowledge.mapper;

import com.guruai.common.enums.MasteryLevel;
import com.guruai.knowledge.dto.response.MasteryProfileResponse;
import com.guruai.knowledge.dto.response.TopicMasteryResponse;
import com.guruai.knowledge.entity.TopicMastery;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MasteryMapper {

    public TopicMasteryResponse toResponse(TopicMastery entity) {
        return new TopicMasteryResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getSessionId(),
                entity.getSubject(),
                entity.getTopic(),
                entity.getEmaScore(),
                entity.getCorrectCount(),
                entity.getTotalCount(),
                entity.getMasteryLevel(),
                entity.getLastUpdated()
        );
    }

    public MasteryProfileResponse toProfileResponse(UUID userId, List<TopicMastery> topics) {
        List<TopicMasteryResponse> responses = topics.stream().map(this::toResponse).toList();
        int weakCount = (int) topics.stream().filter(t -> t.getMasteryLevel() == MasteryLevel.WEAK).count();
        int strongCount = (int) topics.stream().filter(t -> t.getMasteryLevel() == MasteryLevel.STRONG).count();
        int averageCount = topics.size() - weakCount - strongCount;
        double avgPct = topics.isEmpty() ? 0.0 :
                topics.stream().mapToDouble(TopicMastery::getEmaScore).average().orElse(0.0) * 100.0;
        return new MasteryProfileResponse(
                userId, responses, topics.size(), weakCount, averageCount, strongCount,
                Math.round(avgPct * 10.0) / 10.0
        );
    }
}
