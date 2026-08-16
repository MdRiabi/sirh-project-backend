package com.sirh.model.dto;

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
}
