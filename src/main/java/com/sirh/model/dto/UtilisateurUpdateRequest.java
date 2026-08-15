package com.sirh.model.dto;

import com.sirh.model.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UtilisateurUpdateRequest(
        @NotNull Role role,
        @NotNull Boolean compteActif
) {
}