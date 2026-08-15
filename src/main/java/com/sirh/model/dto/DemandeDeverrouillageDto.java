package com.sirh.model.dto;

import com.sirh.model.entity.DemandeDeverrouillage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DemandeDeverrouillageDto(
        Long id,
        LocalDate mois,
        String motif,
        UUID demandeParId,
        String demandeParNom,
        String demandeParPrenom,
        Long departementId,
        String statut,
        UUID traiteParId,
        LocalDateTime dateTraitement) {

    // departementId = celui du demandeur (pas de département sur cloture_paie,
    // qui reste globale par mois — cf. CloturePaieService), utilisé côté
    // frontend pour filtrer la liste par le département affiché à l'écran.
    public static DemandeDeverrouillageDto from(DemandeDeverrouillage demande, String demandeParNom,
                                                  String demandeParPrenom, Long departementId) {
        return new DemandeDeverrouillageDto(demande.getId(), demande.getMois(), demande.getMotif(),
                demande.getDemandeParId(), demandeParNom, demandeParPrenom, departementId,
                demande.getStatut(), demande.getTraiteParId(), demande.getDateTraitement());
    }
}
