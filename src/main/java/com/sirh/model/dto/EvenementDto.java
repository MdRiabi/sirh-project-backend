package com.sirh.model.dto;

import com.sirh.model.enums.EvenementCategorie;
import com.sirh.model.entity.EvenementHistorique;
import com.sirh.model.enums.StatutBadge;

import java.time.LocalDateTime;
import java.util.UUID;

public record EvenementDto(
        UUID reference,
        UUID employeId,
        String titre,
        String description,
        EvenementCategorie categorie,
        StatutBadge statutBadge,
        Long saisiParId,
        LocalDateTime dateEvenement,
        LocalDateTime createdAt
) {
    public static EvenementDto from(EvenementHistorique evenement) {
        return new EvenementDto(
                evenement.getReference(),
                evenement.getEmployeId(),
                evenement.getTitre(),
                evenement.getDescription(),
                evenement.getCategorie(),
                evenement.getStatutBadge(),
                evenement.getSaisiParId(),
                evenement.getDateEvenement(),
                evenement.getCreatedAt()
        );
    }
}