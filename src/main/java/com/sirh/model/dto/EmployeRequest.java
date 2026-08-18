package com.sirh.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EmployeRequest(
    @NotBlank String matriculeInterne,
    String matriculeCnss,
    String matriculePaie,
    @NotBlank String nom,
    @NotBlank String prenom,
    @Email String email,
    Long departementId,
    @NotBlank String typeContrat,
    @NotNull LocalDate dateEmbauche,
    LocalDate dateFinContrat,
    Boolean actif) {}
