package com.sirh.model.dto;

import com.sirh.model.enums.TacheRenouvellementStatut;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// dateFin optionnelle : si omise, le service calcule la date de fin du
// nouveau contrat via le paramètre dynamique DUREE_RENOUVELLEMENT_CDD_MOIS
// (arbitrage Lot 9). Ignorée si statut = NON_RENOUVELE.
public record TacheRenouvellementTraiterRequest(
        @NotNull TacheRenouvellementStatut statut,
        LocalDate dateFin
) {
}
