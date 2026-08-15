package com.sirh.model.dto;

import com.sirh.model.entity.EvaluationCompetence;

import java.math.BigDecimal;

public record EvaluationCompetenceDto(
        Long id,
        String competence,
        BigDecimal note,
        BigDecimal seuilExigence,
        String commentaire
) {
    public static EvaluationCompetenceDto from(EvaluationCompetence entity) {
        return new EvaluationCompetenceDto(entity.getId(), entity.getCompetence(), entity.getNote(),
                entity.getSeuilExigence(), entity.getCommentaire());
    }
}
