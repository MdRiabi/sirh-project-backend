package com.sirh.model.dto;

import com.sirh.model.entity.JourFerie;
import com.sirh.model.enums.TypeJourFerie;

import java.time.LocalDate;

public record JourFerieDto(
        Long id,
        LocalDate date,
        String libelle,
        TypeJourFerie type,
        Boolean national,
        String region
) {
    public static JourFerieDto from(JourFerie j) {
        return new JourFerieDto(j.getId(), j.getDate(), j.getLibelle(), j.getType(), j.getNational(), j.getRegion());
    }
}
