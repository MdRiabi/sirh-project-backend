package com.sirh.model.dto;

import com.sirh.model.entity.Evaluation;
import com.sirh.model.entity.EvaluationCompetence;
import com.sirh.model.enums.EvaluationStatut;
import com.sirh.model.entity.PlanDeveloppement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EvaluationDto(
        Long id,
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
    public static EvaluationDto from(Evaluation evaluation, String employeNom, String employePrenom,
                                      List<EvaluationCompetence> competences, List<PlanDeveloppement> plansDeveloppement) {
        List<EvaluationCompetenceDto> competenceDtos = competences.stream().map(EvaluationCompetenceDto::from).toList();
        List<String> manquantes = competences.stream()
                .filter(c -> c.getNote() == null)
                .map(EvaluationCompetence::getCompetence)
                .toList();
        return new EvaluationDto(evaluation.getId(), evaluation.getEmployeId(), employeNom, employePrenom,
                evaluation.getManagerId(), evaluation.getAnnee(), evaluation.getStatut(), evaluation.getDateEntretien(),
                evaluation.getDateValidationRh(), evaluation.getCreatedAt(), competenceDtos, manquantes,
                plansDeveloppement.stream().map(PlanDeveloppementDto::from).toList());
    }
}
