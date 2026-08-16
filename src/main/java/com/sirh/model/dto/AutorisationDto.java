package com.sirh.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record AutorisationDto(
        UUID reference,
        UUID employeId,
        String employeNom,
        String employePrenom,
        String type,
        LocalDate dateAutorisation,
        LocalTime heureDebut,
        LocalTime heureFin,
        Integer dureeMinutes,
        String motif,
        String statut,
        UUID creeParId,
        String creeParRole,
        String avisManagerStatut,
        String avisManagerCommentaire,
        UUID valideParRhId,
        Integer quotaDepasseMinutes,
        LocalDateTime createdAt
) {
}
