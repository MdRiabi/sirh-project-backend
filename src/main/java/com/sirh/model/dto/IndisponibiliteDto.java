package com.sirh.model.dto;

import com.sirh.model.entity.Indisponibilite;

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
    public static IndisponibiliteDto from(Indisponibilite i, String employeNom, String employePrenom, Integer joursOuvresDecomptes) {
        return new IndisponibiliteDto(
                i.getReference(), i.getEmployeId(), employeNom, employePrenom, i.getTypeIndisponibilite(),
                i.getDateDebut(), i.getDateFin(), i.getStatut(), i.getCommentaire(), i.getPieceJointeId(),
                i.getCreeParId(), i.getCreeParRole(), i.getAvisManagerStatut(), i.getAvisManagerCommentaire(),
                i.getValideParRhId(), joursOuvresDecomptes);
    }
}
