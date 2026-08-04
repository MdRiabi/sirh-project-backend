package com.sirh.parametres.dto;

import jakarta.validation.constraints.NotBlank;

public record ParametreGlobalUpdateRequest(
        @NotBlank String valeur
) {
}
