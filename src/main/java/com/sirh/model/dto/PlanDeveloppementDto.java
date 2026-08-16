package com.sirh.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanDeveloppementDto(
        UUID reference,
        String competence,
        String actions,
        LocalDateTime dateCreation,
        String statut
) {
}
