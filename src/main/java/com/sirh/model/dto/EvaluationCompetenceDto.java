package com.sirh.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record EvaluationCompetenceDto(
        UUID reference,
        String competence,
        BigDecimal note,
        BigDecimal seuilExigence,
        String commentaire
) {
}
