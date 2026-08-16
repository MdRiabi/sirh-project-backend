package com.sirh.model.dto;

import com.sirh.model.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UtilisateurDto(
        UUID reference,
        UUID employeId,
        String employeNom,
        String employePrenom,
        String username,
        Role role,
        Boolean compteActif,
        LocalDateTime dernierLogin,
        LocalDateTime verrouilleJusquA,
        LocalDateTime createdAt
) {
}