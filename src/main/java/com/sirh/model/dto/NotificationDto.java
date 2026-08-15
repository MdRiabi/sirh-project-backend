package com.sirh.model.dto;

import com.sirh.model.entity.Notification;
import com.sirh.model.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDto(
        UUID reference,
        NotificationType type,
        String message,
        String entiteType,
        Long entiteId,
        Boolean lue,
        LocalDateTime createdAt
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getReference(), n.getType(), n.getMessage(),
                n.getEntiteType(), n.getEntiteId(), n.getLue(), n.getCreatedAt());
    }
}
