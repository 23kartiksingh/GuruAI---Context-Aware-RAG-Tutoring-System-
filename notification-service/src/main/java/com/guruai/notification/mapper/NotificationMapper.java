package com.guruai.notification.mapper;

import com.guruai.notification.dto.response.NotificationResponse;
import com.guruai.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification entity) {
        return new NotificationResponse(
                entity.getId(), entity.getUserId(), entity.getType(),
                entity.getTitle(), entity.getMessage(),
                entity.isRead(), entity.getSessionId(), entity.getTopic(),
                entity.getCreatedAt()
        );
    }
}
