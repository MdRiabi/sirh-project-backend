package com.sirh.auth.dto;

import com.sirh.auth.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UtilisateurUpdateRequest(
        @NotNull Role role,
        @NotNull Boolean compteActif
) {
}