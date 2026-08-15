package com.sirh.model.dto;

import com.sirh.model.entity.Autorisation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record AutorisationDto(
        Long id,
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
    public static AutorisationDto from(Autorisation a, String employeNom, String employePrenom, Integer quotaDepasseMinutes) {
        return new AutorisationDto(
                a.getId(), a.getEmployeId(), employeNom, employePrenom, a.getType(), a.getDateAutorisation(),
                a.getHeureDebut(), a.getHeureFin(), a.getDureeMinutes(), a.getMotif(), a.getStatut(),
                a.getCreeParId(), a.getCreeParRole(), a.getAvisManagerStatut(), a.getAvisManagerCommentaire(),
                a.getValideParRhId(), quotaDepasseMinutes, a.getCreatedAt());
    }
}
