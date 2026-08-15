package com.sirh.model.dto;

import com.sirh.model.entity.Contrat;
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
    public static ContratDto from(Contrat contrat) {
        return new ContratDto(
                contrat.getReference(),
                contrat.getEmployeId(),
                contrat.getType(),
                contrat.getDateDebut(),
                contrat.getDateFin(),
                contrat.getStatut(),
                contrat.getMotifFin(),
                contrat.getCreatedAt()
        );
    }
}