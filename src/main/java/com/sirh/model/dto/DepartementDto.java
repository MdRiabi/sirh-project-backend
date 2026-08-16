package com.sirh.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DepartementDto(
        UUID reference,
        String nom,
        UUID managerId,
        LocalDateTime createdAt
) {
}
