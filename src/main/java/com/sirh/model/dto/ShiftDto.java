package com.sirh.model.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ShiftDto(
        UUID reference,
        String nom,
        String typeRotation,
        LocalTime heureDebut,
        LocalTime heureFin,
        LocalDateTime createdAt
) {
}