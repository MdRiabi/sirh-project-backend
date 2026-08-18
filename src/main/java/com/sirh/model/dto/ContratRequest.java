package com.sirh.model.dto;

import com.sirh.model.enums.ContratStatut;
import com.sirh.model.enums.ContratType;
import com.sirh.model.enums.MotifFin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ContratRequest(
    @NotNull ContratType type,
    @NotNull LocalDate dateDebut,
    LocalDate dateFin,
    ContratStatut statut,
    MotifFin motifFin) {}
