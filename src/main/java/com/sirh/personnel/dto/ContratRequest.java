package com.sirh.personnel.dto;

import com.sirh.personnel.entity.ContratStatut;
import com.sirh.personnel.entity.ContratType;
import com.sirh.personnel.entity.MotifFin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ContratRequest(
        @NotNull ContratType type,
        @NotNull LocalDate dateDebut,
        LocalDate dateFin,
        ContratStatut statut,
        MotifFin motifFin
) {
}