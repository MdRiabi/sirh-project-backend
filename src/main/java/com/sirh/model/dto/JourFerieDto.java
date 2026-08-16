package com.sirh.model.dto;

import com.sirh.model.enums.TypeJourFerie;

import java.time.LocalDate;
import java.util.UUID;

public record JourFerieDto(
        UUID reference,
        LocalDate date,
        String libelle,
        TypeJourFerie type,
        Boolean national,
        String region
) {
}
