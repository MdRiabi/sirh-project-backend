package com.sirh.evaluation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EvaluationUpdateRequest(
        @NotEmpty @Valid List<EvaluationCompetenceRequest> competences
) {
}
