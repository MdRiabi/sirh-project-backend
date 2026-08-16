package com.sirh.model.dto;

import com.sirh.model.enums.EvaluationStatut;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EvaluationDto(
        UUID reference,
        UUID employeId,
        String employeNom,
        String employePrenom,
        UUID managerId,
        Integer annee,
        EvaluationStatut statut,
        LocalDate dateEntretien,
        LocalDateTime dateValidationRh,
        LocalDateTime createdAt,
        List<EvaluationCompetenceDto> competences,
        List<String> competencesManquantes,
        List<PlanDeveloppementDto> plansDeveloppement
) {
}
