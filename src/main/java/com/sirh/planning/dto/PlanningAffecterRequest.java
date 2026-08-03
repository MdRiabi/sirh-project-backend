package com.sirh.planning.dto;

import com.sirh.planning.entity.PlanningStatut;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanningAffecterRequest(
        @NotNull UUID employeId,
        @NotNull Long shiftId,
        @NotNull LocalDateTime dateDebut,
        @NotNull LocalDateTime dateFin,
        PlanningStatut statut,
        String commentaire
) {
}