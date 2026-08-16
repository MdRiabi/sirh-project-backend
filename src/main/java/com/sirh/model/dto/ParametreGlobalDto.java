package com.sirh.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ParametreGlobalDto(
        UUID reference,
        String cle,
        String valeur,
        String type,
        String description,
        LocalDateTime updatedAt
) {
}
