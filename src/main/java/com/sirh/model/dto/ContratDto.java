package com.sirh.model.dto;

import com.sirh.model.enums.ContratStatut;
import com.sirh.model.enums.ContratType;
import com.sirh.model.enums.MotifFin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContratDto(
        UUID reference,
        UUID employeId,
        ContratType type,
        LocalDate dateDebut,
        LocalDate dateFin,
        ContratStatut statut,
        MotifFin motifFin,
        LocalDateTime createdAt
) {
}