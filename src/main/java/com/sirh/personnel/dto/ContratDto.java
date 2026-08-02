package com.sirh.personnel.dto;

import com.sirh.personnel.entity.Contrat;
import com.sirh.personnel.entity.ContratStatut;
import com.sirh.personnel.entity.ContratType;
import com.sirh.personnel.entity.MotifFin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContratDto(
        Long id,
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
                contrat.getId(),
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