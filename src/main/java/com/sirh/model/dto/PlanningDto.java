package com.sirh.model.dto;

import com.sirh.model.enums.PlanningStatut;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanningDto(
        UUID reference,
        UUID employeId,
        String employeNom,
        String employePrenom,
        Long shiftId,
        String shiftNom,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        PlanningStatut statut,
        String commentaire,
        LocalDateTime createdAt
) {
}