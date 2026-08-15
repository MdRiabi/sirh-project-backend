package com.sirh.model.dto;

import com.sirh.model.enums.EvenementCategorie;
import com.sirh.model.enums.StatutBadge;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EvenementRequest(
        @NotBlank String titre,
        String description,
        @NotNull EvenementCategorie categorie,
        StatutBadge statutBadge,
        @NotNull LocalDateTime dateEvenement
) {
}