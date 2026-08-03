package com.sirh.calendrier.dto;

import com.sirh.calendrier.entity.TypeJourFerie;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JourFerieRequest(
        @NotNull LocalDate date,
        @NotBlank String libelle,
        @NotNull TypeJourFerie type,
        Boolean national,
        String region
) {
}
