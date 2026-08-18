package com.sirh.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

// departementId volontairement absent : forcé au département du Manager
// créateur (cf. OutillageController), même convention que POST /autorisations
// côté MANAGER.
public record OutillageCreateRequest(
    @NotBlank String codeBarres,
    @NotBlank String nomOutil,
    @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal valeurAchat,
    @NotNull LocalDate dateAchat) {}
