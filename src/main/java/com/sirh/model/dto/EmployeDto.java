package com.sirh.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeDto(
        UUID id,
        String matriculeInterne,
        String matriculeCnss,
        String matriculePaie,
        String nom,
        String prenom,
        String email,
        Long departementId,
        String departementNom,
        String typeContrat,
        LocalDate dateEmbauche,
        LocalDate dateFinContrat,
        BigDecimal soldeCongesJours,
        Boolean actif,
        LocalDateTime createdAt
) {
}