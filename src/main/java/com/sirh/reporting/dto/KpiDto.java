package com.sirh.reporting.dto;

import java.math.BigDecimal;

// Vue consolidée du Dashboard (Module 10, Lot 11) : composé en lecture seule à
// partir des repositories de chaque module métier, aucune nouvelle FK.
// tauxAnomaliesJustifieesDernierMois est nullable (aucune clôture de paie
// encore réalisée en début de vie de l'application).
public record KpiDto(
        long effectifActif,
        long effectifTotal,
        BigDecimal tauxTurnoverMensuel,
        long congesEnCours,
        long alertesPointageNonJustifiees,
        long renouvellementsUrgents,
        long evaluationsEnCours,
        long evaluationsValidees,
        BigDecimal tauxAnomaliesJustifieesDernierMois
) {
}
