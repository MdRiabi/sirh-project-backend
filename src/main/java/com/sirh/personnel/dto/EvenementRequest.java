package com.sirh.personnel.dto;

import com.sirh.personnel.entity.EvenementCategorie;
import com.sirh.personnel.entity.StatutBadge;
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