package com.sirh.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EvaluationCompetenceRequest(
    @NotBlank String competence,
    @DecimalMin(value = "0.0", message = "La note ne peut pas être négative") BigDecimal note,
    @NotNull @DecimalMin(value = "0.0", message = "Le seuil d'exigence ne peut pas être négatif") BigDecimal seuilExigence,
    String commentaire) {}
