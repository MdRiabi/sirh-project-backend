package com.sirh.model.dto;

import com.sirh.model.entity.PlanDeveloppement;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanDeveloppementDto(
        UUID reference,
        String competence,
        String actions,
        LocalDateTime dateCreation,
        String statut
) {
    public static PlanDeveloppementDto from(PlanDeveloppement entity) {
        return new PlanDeveloppementDto(entity.getReference(), entity.getCompetence(), entity.getActions(),
                entity.getCreatedAt(), entity.getStatut());
    }
}
