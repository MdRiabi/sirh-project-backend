package com.sirh.model.dto;

import com.sirh.model.entity.JourFerie;
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
    public static JourFerieDto from(JourFerie j) {
        return new JourFerieDto(j.getReference(), j.getDate(), j.getLibelle(), j.getType(), j.getNational(), j.getRegion());
    }
}
