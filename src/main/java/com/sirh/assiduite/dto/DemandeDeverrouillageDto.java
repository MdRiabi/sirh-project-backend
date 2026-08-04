package com.sirh.assiduite.dto;

import com.sirh.assiduite.entity.DemandeDeverrouillage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DemandeDeverrouillageDto(
        Long id,
        LocalDate mois,
        String motif,
        UUID demandeParId,
        String statut,
        UUID traiteParId,
        LocalDateTime dateTraitement) {

    public static DemandeDeverrouillageDto from(DemandeDeverrouillage demande) {
        return new DemandeDeverrouillageDto(demande.getId(), demande.getMois(), demande.getMotif(),
                demande.getDemandeParId(), demande.getStatut(), demande.getTraiteParId(), demande.getDateTraitement());
    }
}
