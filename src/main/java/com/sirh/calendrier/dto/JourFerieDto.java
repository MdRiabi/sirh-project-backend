package com.sirh.calendrier.dto;

import com.sirh.calendrier.entity.JourFerie;
import com.sirh.calendrier.entity.TypeJourFerie;

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
