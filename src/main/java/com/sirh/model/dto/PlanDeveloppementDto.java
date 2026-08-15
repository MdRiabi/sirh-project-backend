package com.sirh.model.dto;

import com.sirh.model.entity.PlanDeveloppement;

import java.time.LocalDateTime;

public record PlanDeveloppementDto(
        Long id,
        String competence,
        String actions,
        LocalDateTime dateCreation,
        String statut
) {
    public static PlanDeveloppementDto from(PlanDeveloppement entity) {
        return new PlanDeveloppementDto(entity.getId(), entity.getCompetence(), entity.getActions(),
                entity.getCreatedAt(), entity.getStatut());
    }
}
