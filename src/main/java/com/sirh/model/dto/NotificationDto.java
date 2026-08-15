package com.sirh.model.dto;

import com.sirh.model.entity.Notification;
import com.sirh.model.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        NotificationType type,
        String message,
        String entiteType,
        Long entiteId,
        Boolean lue,
        LocalDateTime createdAt
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getMessage(),
                n.getEntiteType(), n.getEntiteId(), n.getLue(), n.getCreatedAt());
    }
}
