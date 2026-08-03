package com.sirh.planning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Rapport croisé planning théorique / pointage réel (cahier-fonctionnel.md
// Module 2 — "Génération d'un rapport croisé (Delta)").
public record DeltaJourDto(
        UUID employeId,
        String employeNom,
        String employePrenom,
        LocalDate jour,
        BigDecimal heuresTheoriques,
        BigDecimal heuresReelles,
        BigDecimal delta
) {
}