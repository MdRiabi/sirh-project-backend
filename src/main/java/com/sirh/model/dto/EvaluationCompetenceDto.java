package com.sirh.model.dto;

import com.sirh.model.entity.EvaluationCompetence;

import java.math.BigDecimal;
import java.util.UUID;

public record EvaluationCompetenceDto(
        UUID reference,
        String competence,
        BigDecimal note,
        BigDecimal seuilExigence,
        String commentaire
) {
    public static EvaluationCompetenceDto from(EvaluationCompetence entity) {
        return new EvaluationCompetenceDto(entity.getReference(), entity.getCompetence(), entity.getNote(),
                entity.getSeuilExigence(), entity.getCommentaire());
    }
}
