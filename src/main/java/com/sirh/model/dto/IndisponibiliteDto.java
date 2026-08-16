package com.sirh.model.dto;

import java.time.LocalDate;
import java.util.UUID;

public record IndisponibiliteDto(
        UUID reference,
        UUID employeId,
        String employeNom,
        String employePrenom,
        String typeIndisponibilite,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut,
        String commentaire,
        Long pieceJointeId,
        UUID creeParId,
        String creeParRole,
        String avisManagerStatut,
        String avisManagerCommentaire,
        UUID valideParRhId,
        Integer joursOuvresDecomptes
) {
}
