package com.sirh.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// departementId = celui du demandeur (pas de département sur cloture_paie,
// qui reste globale par mois — cf. CloturePaieService), utilisé côté
// frontend pour filtrer la liste par le département affiché à l'écran.
public record DemandeDeverrouillageDto(
        UUID reference,
        LocalDate mois,
        String motif,
        UUID demandeParId,
        String demandeParNom,
        String demandeParPrenom,
        Long departementId,
        String statut,
        UUID traiteParId,
        LocalDateTime dateTraitement) {
}
