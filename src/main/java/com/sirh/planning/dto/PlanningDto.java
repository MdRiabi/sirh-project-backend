package com.sirh.planning.dto;

import com.sirh.planning.entity.Planning;
import com.sirh.planning.entity.PlanningStatut;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanningDto(
        Long id,
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
    public static PlanningDto from(Planning p, String employeNom, String employePrenom, String shiftNom) {
        return new PlanningDto(
                p.getId(), p.getEmployeId(), employeNom, employePrenom,
                p.getShiftId(), shiftNom, p.getDateDebut(), p.getDateFin(),
                p.getStatut(), p.getCommentaire(), p.getCreatedAt());
    }
}